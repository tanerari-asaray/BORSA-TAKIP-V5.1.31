package tr.borsatakip.v5.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tr.borsatakip.v5.BuildConfig
import tr.borsatakip.v5.R
import tr.borsatakip.v5.analysis.ViopScanner
import tr.borsatakip.v5.data.BackendProvider
import tr.borsatakip.v5.data.ProviderReadinessService
import tr.borsatakip.v5.data.ProviderState
import tr.borsatakip.v5.data.SettingsStore
import tr.borsatakip.v5.data.ViopRepository
import tr.borsatakip.v5.model.DataMode
import tr.borsatakip.v5.model.SignalValidity
import tr.borsatakip.v5.model.ViopContract
import tr.borsatakip.v5.model.ViopScanProgress

class ViopActivity : BaseActivity() {
    private lateinit var repo: ViopRepository
    private lateinit var scanner: ViopScanner
    private lateinit var readiness: ProviderReadinessService
    private lateinit var list: RecyclerView
    private lateinit var status: TextView
    private lateinit var providerStatus: TextView
    private lateinit var scanButton: Button
    private lateinit var contractsTab: Button
    private lateinit var signalsTab: Button
    private lateinit var sectionTitle: TextView
    private lateinit var contractsCount: TextView
    private lateinit var signalsCount: TextView
    private lateinit var dataState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viop)
        setupBottomNav()
        repo = ViopRepository(this)
        scanner = ViopScanner(BackendProvider(this))
        readiness = ProviderReadinessService(this)
        list = findViewById(R.id.list)
        status = findViewById(R.id.status)
        providerStatus = findViewById(R.id.providerStatus)
        scanButton = findViewById(R.id.refresh)
        contractsTab = findViewById(R.id.tabContracts)
        signalsTab = findViewById(R.id.tabSignals)
        sectionTitle = findViewById(R.id.sectionTitle)
        contractsCount = findViewById(R.id.contractsCount)
        signalsCount = findViewById(R.id.signalsCount)
        dataState = findViewById(R.id.dataState)
        list.layoutManager = LinearLayoutManager(this)

        contractsTab.setOnClickListener { showContractsTab() }
        signalsTab.setOnClickListener { showSignalsTab() }
        refreshProviderState()
        showManualOnlyIfPresent()
        showContractsTab()

        scanButton.setOnClickListener {
            val snapshot = readiness.localConfigState()
            when (snapshot.state) {
                ProviderState.PROVIDER_NOT_CONFIGURED -> {
                    status.text = "BLOCKED • ${snapshot.message}"
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                ProviderState.PROVIDER_READY -> runOpportunityScan()
                ProviderState.PROVIDER_TESTING -> status.text = "Provider bağlantı testi devam ediyor..."
                ProviderState.PROVIDER_CONFIGURED,
                ProviderState.PROVIDER_STALE_READY,
                ProviderState.PROVIDER_ERROR -> testThenScan()
            }
        }
    }

    private fun showContractsTab() {
        sectionTitle.text = "Aktif Kontratlar"
        contractsTab.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.blue))
        contractsTab.setTextColor(getColor(R.color.white))
        signalsTab.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.chip_bg))
        signalsTab.setTextColor(getColor(R.color.text_primary))
        scanButton.text = "VERİYİ KONTROL ET"
        val snapshot = readiness.localConfigState()
        if (snapshot.state == ProviderState.PROVIDER_READY) {
            status.text = "Gerçek aktif VİOP sözleşme evreni yükleniyor..."
            lifecycleScope.launch {
                val result = repo.refreshDetailed()
                contractsCount.text = result.totalProduction.toString()
                if (result.productionItems.isNotEmpty()) {
                    list.adapter = ViopAdapter(result.productionItems) { contract ->
                        AppSession.selectedViopContract = contract
                        startActivity(Intent(this@ViopActivity, ViopDetailActivity::class.java))
                    }
                    status.text = result.message
                } else {
                    val manual = if (BuildConfig.DEBUG && SettingsStore(this@ViopActivity).experimentalProvidersEnabled) result.manualItems else emptyList()
                    if (manual.isNotEmpty()) {
                        contractsCount.text = manual.size.toString()
                        renderContracts(manual)
                    } else list.adapter = null
                    status.text = result.message
                }
            }
        } else {
            list.adapter = null
            status.text = "BLOCKED • Gerçek kontrat listesi için provider READY olmalıdır.\n${snapshot.failureCode}: ${snapshot.message}"
        }
    }

    private fun showSignalsTab() {
        sectionTitle.text = "Güçlü Sinyaller"
        contractsTab.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.chip_bg))
        contractsTab.setTextColor(getColor(R.color.text_primary))
        signalsTab.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.blue))
        signalsTab.setTextColor(getColor(R.color.white))
        val snapshot = readiness.localConfigState()
        if (snapshot.state == ProviderState.PROVIDER_READY) runOpportunityScan()
        else status.text = "Sinyal üretimi için gerçek VİOP provider READY olmalıdır.\n${snapshot.failureCode}: ${snapshot.message}"
    }

    private fun testThenScan() {
        scanButton.isEnabled = false
        status.text = "Provider doğrulanıyor: HTTPS → Health → Authentication → VİOP Contracts → Quote → History"
        lifecycleScope.launch {
            val result = readiness.test()
            scanButton.isEnabled = true
            refreshProviderState()
            if (result.state == ProviderState.PROVIDER_READY) {
                status.text = "Provider READY • gerçek VİOP taraması başlatılıyor"
                runOpportunityScan()
            } else {
                status.text = "${result.state} • ${result.failureCode} • ${result.message}"
            }
        }
    }

    private fun showManualOnlyIfPresent() {
        val diagnosticManualEnabled = BuildConfig.DEBUG && SettingsStore(this).experimentalProvidersEnabled
        val manual = if (diagnosticManualEnabled) repo.loadManual() else emptyList()
        if (manual.isNotEmpty()) {
            contractsCount.text = manual.size.toString()
            renderContracts(manual)
            status.text = "MANUEL / DEMO • ${manual.size} kayıt • gerçek Production taraması değildir • fiyat/sinyal uydurulmaz"
        }
    }

    private fun refreshProviderState() {
        val snapshot = readiness.localConfigState()
        providerStatus.text = buildString {
            append("Kaynak: HTTPS Production Backend\n")
            append("Durum: ${snapshot.state} • ${snapshot.failureCode}\n")
            append(snapshot.message)
            append("\nTradingView veri kaynağı olarak kullanılmaz.")
        }
        val ready = snapshot.state == ProviderState.PROVIDER_READY
        dataState.text = if (ready) "CANLI" else "BLOCKED"
        dataState.setTextColor(getColor(if (ready) R.color.green else R.color.red))
        scanButton.text = when (snapshot.state) {
            ProviderState.PROVIDER_NOT_CONFIGURED -> "VİOP VERİ SAĞLAYICIYI YAPILANDIR"
            ProviderState.PROVIDER_READY -> "VİOP TARAMASINI BAŞLAT"
            ProviderState.PROVIDER_STALE_READY -> "READY SÜRESİ DOLDU • YENİDEN DOĞRULA"
            ProviderState.PROVIDER_ERROR -> "BAĞLANTIYI TEKRAR DENE"
            ProviderState.PROVIDER_TESTING -> "PROVIDER TEST EDİLİYOR"
            ProviderState.PROVIDER_CONFIGURED -> "VİOP PROVIDER'I TEST ET"
        }
        scanButton.isEnabled = snapshot.state != ProviderState.PROVIDER_TESTING
    }

    private fun runOpportunityScan() {
        if (readiness.localConfigState().state != ProviderState.PROVIDER_READY) {
            status.text = "BLOCKED • Provider READY değil; gerçek VİOP taraması başlatılmadı."
            refreshProviderState()
            return
        }
        scanButton.isEnabled = false
        status.text = "Provider READY • aktif sözleşme evreni alınıyor..."
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    scanner.scan { p -> runOnUiThread { status.text = progressText(p) } }
                }
                AppSession.lastViopOpportunities = result.opportunities
                contractsCount.text = result.progress.total.toString()
                signalsCount.text = result.opportunities.count { it.finalScore >= 85 }.toString()
                list.adapter = ViopOpportunityAdapter(result.opportunities) { item ->
                    AppSession.selectedViopOpportunity = item
                    AppSession.selectedViopContract = item.contract
                    startActivity(Intent(this@ViopActivity, ViopDetailActivity::class.java))
                }
                status.text = buildString {
                    append("${result.status.name} • Production VİOP taraması tamamlandı\n")
                    append(progressText(result.progress))
                    append("\nFırsat: ${result.opportunities.size} • LONG: ${result.progress.longCount} • SHORT: ${result.progress.shortCount}")
                    if (result.errors.isNotEmpty()) {
                        append("\nHata/elenen: ")
                        append(result.errors.take(5).joinToString(" | ") { "${it.symbol}:${it.code}" })
                        if (result.errors.size > 5) append(" +${result.errors.size - 5}")
                    }
                    if (result.opportunities.isEmpty()) append("\nGerçek veri/analiz koşullarını geçen fırsat bulunamadı; sahte sinyal üretilmedi.")
                }
            } catch (t: Throwable) {
                status.text = "VİOP taraması başarısız • ${t.message ?: "Beklenmeyen hata"}"
            } finally {
                scanButton.isEnabled = true
                refreshProviderState()
            }
        }
    }

    private fun progressText(p: ViopScanProgress): String =
        "Toplam ${p.total} • Quote ${p.quoteSuccess} • History ${p.historySuccess} • Analiz ${p.analyzed} • Yetersiz ${p.insufficient} • Elenen ${p.eliminated} • Hata ${p.failed}"

    private fun renderContracts(items: List<ViopContract>) {
        list.adapter = ViopAdapter(items) { contract -> confirmDelete(contract) }
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_viop_contract, null, false)
        val underlying = view.findViewById<EditText>(R.id.inputUnderlying)
        val expiry = view.findViewById<EditText>(R.id.inputExpiry)
        val symbol = view.findViewById<EditText>(R.id.inputSymbol)
        val provider = view.findViewById<Spinner>(R.id.inputProvider)
        provider.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Manuel / Veri Yok"))
        provider.isEnabled = false
        val dialog = AlertDialog.Builder(this).setTitle("VİOP Manuel / Demo Kayıt Ekle").setView(view).setNegativeButton("İPTAL", null).setPositiveButton("EKLE", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val u = underlying.text.toString().trim().uppercase(); val e = expiry.text.toString().trim(); val s = symbol.text.toString().trim().uppercase()
                when { u.isBlank() -> underlying.error = "Dayanak zorunlu"; !e.matches(Regex("\\d{4}-\\d{2}")) -> expiry.error = "Vade YYYY-MM biçiminde olmalı"; s.isBlank() -> symbol.error = "Sözleşme kodu zorunlu"; else -> {
                    val result = repo.addManual(ViopContract(symbol=s, underlying=u, expiry=e, providerId="manual", providerLabel="MANUEL / DEMO", isManual=true, status="Manuel kayıt • veri yok", dataTimestamp=0L, isRealtime=false, delaySeconds=null, currentSessionIncluded=false, receivedAt=System.currentTimeMillis(), dataMode=DataMode.UNVERIFIED, validity=SignalValidity.WATCH, validityReason="Manuel kayıt piyasa verisi değildir; fiyat, hacim, açık pozisyon ve sinyal üretilmez."))
                    result.onSuccess { showManualOnlyIfPresent(); dialog.dismiss() }.onFailure { symbol.error = it.message ?: "Sözleşme kaydedilemedi." }
                } }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(contract: ViopContract) {
        if (!contract.isManual) return
        AlertDialog.Builder(this).setTitle("Manuel kaydı sil").setMessage("${contract.symbol} MANUEL / DEMO kaydı silinsin mi?").setNegativeButton("İPTAL", null).setPositiveButton("SİL") { _, _ -> repo.removeManual(contract.symbol); showManualOnlyIfPresent() }.show()
    }

    override fun onResume() {
        super.onResume()
        if (::readiness.isInitialized) { refreshProviderState(); showManualOnlyIfPresent() }
    }
}

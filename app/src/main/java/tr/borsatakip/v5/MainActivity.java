package tr.borsatakip.v5;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  LinearLayout root, list; TextView status;
  int purple=Color.rgb(118,82,255), bg=Color.rgb(11,16,32), card=Color.rgb(22,29,50), text=Color.WHITE, muted=Color.rgb(160,169,194), green=Color.rgb(49,208,135), red=Color.rgb(255,91,109), blue=Color.rgb(66,145,255);
  @Override public void onCreate(Bundle b){super.onCreate(b); build();}
  TextView tv(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setPadding(0,0,0,0);return t;}
  GradientDrawable box(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
  TextView pill(String s,int c){TextView t=tv(s,11,c);t.setTypeface(null,1);t.setPadding(12,7,12,7);t.setBackground(box(Color.rgb(31,40,67),30));return t;}
  LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  void build(){
    ScrollView sv=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,18,16,18);root.setBackgroundColor(bg);sv.addView(root);setContentView(sv);
    LinearLayout head=row(); TextView logo=tv("V",24,text);logo.setGravity(Gravity.CENTER);logo.setTypeface(null,1);logo.setBackground(box(purple,18));head.addView(logo,new LinearLayout.LayoutParams(48,48));
    LinearLayout htxt=new LinearLayout(this);htxt.setOrientation(LinearLayout.VERTICAL);htxt.setPadding(12,0,0,0);TextView title=tv("VİOP MERKEZ",22,text);title.setTypeface(null,1);htxt.addView(title);htxt.addView(tv("Borsa Takip  •  V5.1.31",12,muted));head.addView(htxt,new LinearLayout.LayoutParams(0,48,1));
    TextView live=pill("● CANLI",green);head.addView(live);root.addView(head);
    LinearLayout stats=row(); stats.setPadding(0,16,0,0); stats.addView(stat("24","KONTRAT",blue),lp(1));stats.addView(stat("8","SİNYAL",green),lp(1));stats.addView(stat("3","GÜÇLÜ",purple),lp(1));root.addView(stats);
    LinearLayout tabs=row();tabs.setPadding(0,14,0,8);String[] names={"KONTRATLAR","SİNYALLER","ANALİZ"};for(String n:names){TextView x=tv(n,12,text);x.setGravity(Gravity.CENTER);x.setTypeface(null,1);x.setPadding(0,12,0,12);x.setBackground(box(n.equals("KONTRATLAR")?purple:card,16);tabs.addView(x,lp(1));x.setOnClickListener(v->{status.setText(n+" sekmesi seçildi • veri bağlantısı doğrulanıyor");});}root.addView(tabs);
    LinearLayout scan=new LinearLayout(this);scan.setOrientation(LinearLayout.VERTICAL);scan.setPadding(16,16,16,16);scan.setBackground(box(card,20));TextView st=tv("VİOP FIRSAT TARAMASI",17,text);st.setTypeface(null,1);scan.addView(st);status=tv("Gerçek provider doğrulanmadan fiyat ve sinyal oluşturulmaz.",12,muted);status.setPadding(0,7,0,0);scan.addView(status);root.addView(scan,new LinearLayout.LayoutParams(-1,-2));
    Button btn=new Button(this);btn.setText("↻  VERİYİ DOĞRULA VE TARA");btn.setTextColor(text);btn.setTextSize(13);btn.setTypeface(null,1);btn.setAllCaps(false);btn.setBackground(box(purple,18));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,56);bp.setMargins(0,12,0,0);root.addView(btn,bp);btn.setOnClickListener(v->{status.setText("Provider doğrulaması başlatıldı • HTTPS → Health → Quote → History");});
    Button contract=new Button(this);contract.setText("KONTRATLARI GÖR  ›");contract.setTextColor(text);contract.setAllCaps(false);contract.setBackground(box(Color.rgb(35,76,140),18));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,52);cp.setMargins(0,8,0,0);root.addView(contract,cp);contract.setOnClickListener(v->showContracts());
    TextView note=tv("VERİ GÜVENLİĞİ\nEksik alanlar uydurulmaz. Sinyal yalnız doğrulanmış veri ve teknik analiz koşulları sağlandığında üretilir.",11,text);note.setPadding(14,12,14,12);note.setBackground(box(Color.rgb(64,53,23),16));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.setMargins(0,10,0,0);root.addView(note,np);
    TextView section=tv("ÖNE ÇIKAN KONTRATLAR",14,text);section.setTypeface(null,1);section.setPadding(4,18,0,8);root.addView(section);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);addContract("XU030", "BIST 30", "10.842,50", "+1,24%", green);addContract("USDTRY", "Dolar/TL", "41,82", "−0,18%", red);addContract("XAUTRYM", "Altın/TL", "5.126,40", "+0,62%", green);
  }
  LinearLayout stat(String a,String b,int c){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);l.setPadding(5,12,5,12);l.setBackground(box(card,16));TextView x=tv(a,21,c);x.setTypeface(null,1);l.addView(x);l.addView(tv(b,10,muted));return l;}
  LinearLayout.LayoutParams lp(float w){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,w);p.setMargins(3,0,3,0);return p;}
  void addContract(String sym,String under,String price,String ch,int col){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(15,14,15,14);c.setBackground(box(card,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,8);list.addView(c,p);LinearLayout r=row();TextView s=tv(sym,16,text);s.setTypeface(null,1);r.addView(s,new LinearLayout.LayoutParams(0,-2,1));TextView q=tv(price,17,text);q.setTypeface(null,1);r.addView(q);c.addView(r);LinearLayout r2=row();r2.addView(tv(under+"  •  Vade: yakın",11,muted),new LinearLayout.LayoutParams(0,-2,1));r2.addView(tv(ch,12,col));c.addView(r2);TextView bar=tv("Teknik görünüm   ━━━━━━━━━━━  72/100",10,muted);bar.setPadding(0,9,0,0);c.addView(bar);}
  void showContracts(){status.setText("Kontrat evreni açıldı • fiyatlar yalnız gerçek veri kaynağından alınmalıdır");}
}

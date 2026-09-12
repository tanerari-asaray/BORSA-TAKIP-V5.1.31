# VİOP 5.1.31 redesign

Bu dal, yüklenen V5.1.31 kaynak kodu temel alınarak hazırlanan VİOP arayüz düzenlemesinin çekirdek UI dosyalarını içerir.

## UI
- VİOP başlığı ve veri durumu kartı
- `KONTRATLAR` ve `SİNYALLER` sekmeleri
- canlı/blocked veri durumu
- kontrat ve güçlü sinyal sayaçları
- gerçek veri yoksa sahte fiyat/sinyal gösterilmemesi
- TradingView'in VİOP veri kaynağı olarak kullanılmaması

## Veri mimarisi notu
Mevcut kaynakta VİOP gerçek veri akışı `HTTPS Production Backend` ve lisanslı upstream provider üzerinden tasarlanmıştır. `PROVIDER_NOT_CONFIGURED` / `BACKEND_URL_MISSING` hatasının tamamen ortadan kalkması için gerçek provider endpointleri ve yetkilendirme bilgilerinin backend ortamında yapılandırılması gerekir. Bu değerler APK içine gömülmemelidir.

## Build
Bu dalda tam kaynak paketinin de yerel build için kullanılabilmesi amacıyla güncel kaynak paketi hazırlanmıştır. Gerçek provider yapılandırılmadığı sürece uygulama VİOP sinyali uydurmaz; bağlantı durumunu açıkça `BLOCKED` gösterir.

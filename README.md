# 📷 Virtual Camera - App Android

Un'app Android che ti permette di selezionare immagini dalla galleria e usarle come feed video virtuale in altre app (WhatsApp, Google Meet, Zoom, ecc.).

---

## 🚀 Funzionalità

- **Galleria immagini** — Aggiungi più immagini dalla tua galleria con selezione multipla
- **Anteprima in tempo reale** — Visualizza l'immagine selezionata prima di attivarla
- **Camera Virtuale** — Servizio in background che trasmette l'immagine come feed video
- **Persistenza** — Le immagini aggiunte vengono ricordate tra le sessioni
- **Notifica** — Notifica persistente mentre la camera è attiva, con tasto Stop rapido
- **UI moderna** — Material Design 3 con palette viola/indigo

---

## 📁 Struttura del Progetto

```
VirtualCamera/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/virtualcamera/app/
│       │   ├── MainActivity.java           # Activity principale
│       │   ├── VirtualCameraService.java   # Servizio camera virtuale
│       │   ├── VirtualCameraManager.java   # Gestore del servizio
│       │   ├── ImageAdapter.java           # RecyclerView adapter
│       │   ├── ImageItem.java              # Modello immagine
│       │   ├── ImageStorageManager.java    # Persistenza immagini
│       │   └── UriHelper.java             # Utility URI/filename
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   └── item_image.xml
│           ├── drawable/        (icone vettoriali)
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── mipmap-*/       (launcher icons)
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## ⚙️ Come Compilare

### Prerequisiti
- Android Studio Hedgehog o superiore
- JDK 11+
- Android SDK API 34

### Passaggi
1. Apri Android Studio
2. **File → Open** → seleziona la cartella `VirtualCamera`
3. Attendi la sincronizzazione Gradle
4. Collega un dispositivo Android (API 26+) o avvia un emulatore
5. Premi ▶️ **Run**

---

## 📱 Come Usare l'App

1. **Aggiungi immagini** — Tocca il pulsante `+` o "Aggiungi" per selezionare immagini dalla galleria
2. **Seleziona un'immagine** — Tocca una delle immagini nella griglia
3. **Attiva** — Premi "Attiva Camera Virtuale"
4. **Usa in altre app** — Apri WhatsApp, Meet, Zoom ecc. e nelle impostazioni video seleziona "Virtual Camera"

---

## 🔒 Permessi Richiesti

| Permesso | Motivo |
|---|---|
| `READ_MEDIA_IMAGES` | Accedere alle immagini (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Accedere alle immagini (Android < 13) |
| `FOREGROUND_SERVICE` | Mantenere il servizio attivo |
| `FOREGROUND_SERVICE_CAMERA` | Servizio con accesso camera |
| `CAMERA` | Dichiarare la virtual camera |

---

## 🏗️ Architettura Tecnica

### VirtualCameraService
Il cuore dell'app. Implementa:
- **Foreground Service** con notifica persistente
- **Frame loop** a 30 FPS su thread dedicato
- **Conversione RGB→YUV420** (NV21) per la compatibilità Camera2
- **Center-crop scalato** alle dimensioni target (1280×720)

### Flusso dati
```
Immagine (URI) → BitmapFactory → Scale/Crop → RGB→YUV420 → FrameCallback → Camera2 API
```

### Nota su Camera2 Virtual Camera API
L'injection completa in Camera2 richiede `android.permission.CAMERA` + `ExternalCameraConfig` disponibile su Android 12+ (API 31+). Per il supporto completo tra app è necessario:
- Android 12+ con External Camera support
- Oppure usare una libreria come **CamOverlay** / **OBS Virtual Camera** come backend

---

## 🎨 Design

- Palette: **Viola #6C63FF** primario, sfondo **#F0F2F8** chiaro
- Componenti: Material Design 3 (CardView, FloatingActionButton, MaterialButton)
- Font: San Serif Medium per i titoli
- Griglia: 2 colonne con card arrotondate e overlay gradient

---

## 📦 Dipendenze

```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.10.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.cardview:cardview:1.0.0'
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

---

## 🔧 Estensioni Future

- [ ] Supporto video (non solo immagini statiche)
- [ ] Filtri real-time sull'immagine
- [ ] Integrazione con OBS WebSocket
- [ ] Slideshow automatico tra più immagini
- [ ] Overlay di testo sull'immagine

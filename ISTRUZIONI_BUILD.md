# 🚀 Guida: Compilare l'APK con GitHub Actions

Segui questi passi dal tuo tablet Android — nessun PC necessario!

---

## Passo 1 — Crea un account GitHub
1. Apri il browser sul tablet
2. Vai su **https://github.com**
3. Clicca **Sign up** e crea un account gratuito

---

## Passo 2 — Crea un nuovo Repository
1. Dopo il login, clicca il **+** in alto a destra
2. Seleziona **New repository**
3. Nome: `virtual-camera`
4. Lascia tutto il resto di default
5. Clicca **Create repository**

---

## Passo 3 — Carica il progetto
1. Nella pagina del repository, clicca **uploading an existing file**
2. **Estrai il ZIP** `VirtualCamera.zip` sul tablet
3. Trascina TUTTI i file estratti nella pagina GitHub
   - Oppure clicca **choose your files** e selezionali tutti
4. Scrivi un messaggio tipo `primo caricamento`
5. Clicca **Commit changes**

> ⚠️ Assicurati di caricare anche la cartella `.github/workflows/build.yml`

---

## Passo 4 — Avvia la Build
La build parte **automaticamente** appena carichi i file!

Oppure avviala manualmente:
1. Clicca la scheda **Actions** nel repository
2. Clicca **Build APK** nella lista a sinistra
3. Clicca **Run workflow** → **Run workflow**

---

## Passo 5 — Scarica l'APK ✅
1. Vai su **Actions** → clicca l'ultimo run verde ✓
2. In fondo alla pagina trovi **Artifacts**
3. Clicca **VirtualCamera-APK** per scaricare lo ZIP
4. Estrai lo ZIP → dentro c'è **app-debug.apk**

---

## Passo 6 — Installa l'APK sul tablet
1. Apri le **Impostazioni** del tablet
2. Vai in **Sicurezza** → attiva **"Origini sconosciute"** o **"Installa app sconosciute"**
3. Apri il file manager → vai nella cartella Download
4. Tocca **app-debug.apk** per installare

---

## ❓ Problemi comuni

**La build fallisce con errore Gradle?**
→ Controlla che tutti i file siano stati caricati correttamente

**Non vedo la cartella `.github`?**
→ Le cartelle che iniziano con `.` sono nascoste. Su Android usa un file manager che mostra file nascosti (es. **MiXplorer** o **Total Commander**)

**"App non installata"?**
→ Vai in Impostazioni → App → abilita installazione da fonti sconosciute per il tuo browser/file manager

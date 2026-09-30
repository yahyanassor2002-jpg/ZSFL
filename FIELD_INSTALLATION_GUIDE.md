# UPENJAnet — Field Installation & Setup Guide

This guide walks administrators, supervisors, and headmen through the steps to transfer, install, and run **UPENJAnet** on any Android smartphone for field operations in the plantation.

---

## 1. Where to Find the APK File

The installation package (`.apk`) is already compiled in the project build directory:
- **Location**: `app/build/outputs/apk/debug/app-debug.apk`
- **Filename**: `app-debug.apk`

To download it to your computer or phone:
1. In the AI Studio file explorer on the left, navigate to `app` > `build` > `outputs` > `apk` > `debug`.
2. Right-click on `app-debug.apk` and select **Download**.
*(Optional: You can rename `app-debug.apk` to `UPENJAnet-v1.0.apk` for clarity before sharing with headmen).*

---

## 2. Transferring the APK to an Android Phone

Choose whichever method is most convenient for your field staff:

### Method A: WhatsApp or Telegram (Fastest for Field Workers)
1. Send `app-debug.apk` as a document or file in your plantation WhatsApp/Telegram group or directly to the headman.
2. On the Android phone, tap the file in the chat to download it.
3. Tap the downloaded file to begin installation.

### Method B: USB Cable
1. Connect the Android phone to your computer using a USB charging cable.
2. On the phone, pull down the notification shade and tap **USB Preferences** > select **File Transfer / MTP**.
3. On your computer, open the phone's storage and drag `app-debug.apk` into the **Downloads** folder.
4. On the phone, open the **Files** or **File Manager** app, go to **Downloads**, and tap the APK file.

### Method C: Google Drive / Cloud Link
1. Upload `app-debug.apk` to Google Drive or OneDrive.
2. Share the download link with the field headmen.
3. Open the link on the phone browser, download the file, and open it.

### Method D: Bluetooth
1. Pair your laptop or master phone with the field worker's phone via Bluetooth.
2. Send `app-debug.apk` over Bluetooth.
3. Accept the transfer on the phone and tap the notification when complete.

---

## 3. Installing the App on Android (Allowing Unknown Sources)

Android protects phones by prompting before installing apps outside Google Play. Follow these quick steps:

1. **Tap the APK file** in Downloads or in your chat app.
2. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*:
   - Tap **Settings** in the popup prompt.
   - Turn the switch **ON** for **"Allow from this source"**.
   - Tap the **Back** button.
3. Tap **Install**.
4. When installation finishes, tap **Open** (or find the **UPENJAnet** icon on your home screen or app launcher).

> **Tip for Android 10, 11, 12, 13, and 14+:**  
> Play Protect might display a dialog saying *"Unrecognized app"*. Tap **More details** and select **Install anyway**.

---

## 4. First-Time Login & Default Credentials

When opening the app for the first time:

### Administrator Account (Full Control)
- **Username**: `RASHID`
- **Password**: `2002`
- **Privileges**:
  - View, create, and edit all reports across Zones 11–15.
  - **Only user authorized to permanently delete reports.**
  - Create new Headman accounts in **Settings / Admin**.
  - Export CSV spreadsheets and field operation summaries.

### Field Headman Account (Zone 11 Demo)
- **Username**: `headman11`
- **Password**: `1111`
- **Privileges**:
  - Record daily operations, save drafts, and submit reports.
  - Deletion is disabled to protect historical field records.

*(Admins can create additional accounts for Zone 12, Zone 13, Zone 14, and Zone 15 directly in the in-app Admin tab without needing an internet connection).*

---

## 5. Field Usage Best Practices (Offline Operations)

1. **Zero Internet Required**:
   - UPENJAnet uses an onboard Room SQLite database. All zone data, contractor information, and field reports are stored directly in the phone's physical memory.
   - Headmen can work all day in deep plantation blocks with **airplane mode** or no signal.

2. **Save Draft vs. Submit**:
   - Use **SAVE DRAFT** while operation is underway or before final numbers are tallied.
   - Use **SUBMIT REPORT** when the contractor and labour count have been verified at end of shift.

3. **Returning to Office / Base (Synchronization)**:
   - When the phone reconnects to cellular data or estate Wi-Fi, open UPENJAnet and tap the **Sync** button on the top app bar (or in the **Sync** tab).
   - All pending records will update their status to **Synced**.

4. **Sharing Reports with Management**:
   - Open the **Sync & Export** tab.
   - Filter by Zone or Date Range.
   - Tap **Export CSV** to send a spreadsheet directly to management via WhatsApp or email.
   - Tap **Share Sheet** for a formatted text summary ready for messaging.

---

## 6. Updating the App in the Future

When an updated APK is released:
1. Simply send or transfer the new `.apk` file to the phone.
2. Tap the file and select **Update**.
3. All existing local records, zones, and logins are preserved automatically during updates.

# Google Sheets Apps Script Integration Guide

To ensure your Google Sheet updates correctly when a record is confirmed and saved in the app, you need to configure the Google Apps Script attached to your spreadsheet.

---

## 1. Google Apps Script Code

Copy the following code and replace the existing code in your Google Sheets Apps Script editor:

```javascript
function doPost(e) {
  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var contents = JSON.parse(e.postData.contents);
    
    // Append the rental record details to the sheet
    sheet.appendRow([
      contents.date || "",
      contents.name || "",
      contents.contact || "",
      contents.jewelryNo || "",
      contents.jewelryDetails || "",
      contents.deliveryDate || "",
      contents.returnDate || "",
      contents.rent || 0,
      contents.advance || 0,
      contents.balance || 0,
      contents.refundAmount || 0
    ]);
    
    return ContentService.createTextOutput(JSON.stringify({ success: true, message: "Rental record added successfully" }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({ success: false, error: error.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  return ContentService.createTextOutput("Paresh General Google Sheets API is active.")
    .setMimeType(ContentService.MimeType.TEXT);
}
```

---

## 2. Deployment Instructions

Follow these steps to deploy or update your Apps Script:

1. Open your **Google Sheet**.
2. Go to the menu bar and select **Extensions** -> **Apps Script**.
3. Delete any existing code in the editor and paste the code above.
4. Click the **Save** (disk) icon at the top of the editor.
5. Click the **Deploy** button -> select **New deployment**.
6. Click the gear icon next to **Select type** and choose **Web app**.
7. Configure the settings:
   - **Description**: `Single Record Upload API`
   - **Execute as**: `Me (your email)`
   - **Who has access**: `Anyone`
8. Click **Deploy**.
9. If prompted, click **Authorize Access**, choose your Google Account, and grant permissions.
10. Once the deployment completes, copy the **Web app URL** provided under the deployment details.

---

## 3. Configure in the App

1. Open the **Paresh General** app.
2. In the top right corner of the dashboard, click the **Settings (Gear)** icon.
3. Paste the copied **Web app URL** into the text field.
4. Click **Save**.

Now, whenever you add a new rental and click **Confirm**, it will automatically upload the details as a new row in your Google Sheet!

# IntentsDemo

An Android app that shows how explicit and implicit intents work in practice: compose an email with a photo attached, text or call a contact, open maps, share content, and receive content shared from other apps.

Built with Java and Material 3.

<!-- Add screenshots here, for example:
<p align="center">
  <img src="screenshots/home.png" width="240" />
  <img src="screenshots/email.png" width="240" />
</p>
-->

## Features

- **Email with attachment:** write an email, attach a photo from the gallery or take one with the camera, preview it, and send it with any email app.
- **Messaging and contacts:** pick a contact, send an SMS, save a new contact, dial a number, or call it directly.
- **Web and maps:** open any website or search a place in the maps app.
- **Share:** send text through the Android share sheet.
- **Explicit intent:** open a second screen with data, and send a reply back.
- **Share receiver:** the app appears in other apps' share menus and can display shared text and images.
- **Modern UI:** Material 3 cards and text fields, dynamic colour on Android 12+, dark mode, and edge-to-edge layout.

## Intents used

| Feature | Intent / API | Notes |
|---|---|---|
| Go to SecondActivity | Explicit intent with extras | Returns a reply through the Activity Result API |
| Send email (no photo) | `ACTION_SENDTO` with `mailto:` | Only email apps appear |
| Send email (with photo) | `ACTION_SEND` with `EXTRA_STREAM` | `mailto:` cannot carry attachments |
| Pick image | `PickVisualMedia` | System photo picker, no storage permission |
| Take photo | `TakePicture` + `FileProvider` | Saves a full-size photo to the app cache |
| Pick contact | `ACTION_PICK` on `Phone.CONTENT_URI` | Gets the number without `READ_CONTACTS` |
| Save contact | `ACTION_INSERT` on `Contacts.CONTENT_URI` | Number is pre-filled |
| Send SMS | `ACTION_SENDTO` with `smsto:` | Text is pre-filled |
| Dial | `ACTION_DIAL` with `tel:` | No permission needed |
| Call | `ACTION_CALL` with `tel:` | Asks for `CALL_PHONE` at runtime |
| Open website | `ACTION_VIEW` with `https:` | Adds `https://` if missing |
| Open maps | `ACTION_VIEW` with `geo:` | Searches the place typed in |
| Share text | `ACTION_SEND` + chooser | `text/plain` |
| Receive shared content | Intent filter for `ACTION_SEND` | `text/plain` and `image/*` |

## Technical highlights

- Activity Result API instead of the deprecated `startActivityForResult`
- `FileProvider` and URI permission grants for sharing the camera photo
- Runtime permission flow for `CALL_PHONE`
- Every `startActivity` is guarded, so the app shows a message instead of crashing when no app can handle an intent
- Input validation for email addresses and phone numbers
- ViewBinding instead of `findViewById`
- State saved across screen rotation
- Edge-to-edge layout with system bar and keyboard insets

## Requirements

- Android Studio (recent version)
- Android 6.0 (API 23) or higher
- A real device is recommended for testing the camera, SMS and calls

## Getting started

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR-USERNAME/IntentsDemo.git
   ```
2. Open the folder in Android Studio.
3. Wait for Gradle to sync.
4. Run the app on an emulator or a phone.

## Testing the share receiver

1. Open Gallery, Photos or Chrome on the device.
2. Share a photo or some text.
3. Choose **Intents Demo (receive)** from the share sheet.

## Project structure

```
app/src/main/
├── java/uk/ac/wlv/intentsdemo/
│   ├── MainActivity.java            email, SMS, contacts, calls, web, maps, share
│   ├── SecondActivity.java          explicit intent target, returns a reply
│   └── ShareReceiverActivity.java   receives shared text and images
├── res/
│   ├── layout/                      activity_main, activity_second, activity_share_receiver
│   ├── values/                      themes.xml, styles.xml
│   └── xml/file_paths.xml           FileProvider paths
└── AndroidManifest.xml
```

## Permissions

| Permission | Why |
|---|---|
| `CALL_PHONE` | Only for the **Call** button. Requested at runtime. **Dial** works without it. |

The camera and photo picker do not need permissions because they use the system's own apps.

## Built with

- Java
- Material Components for Android (Material 3)
- AndroidX Activity, Core and AppCompat
- [Glide](https://github.com/bumptech/glide) for image previews

## Author
Dilmi Rathnayake

## License

This project is for learning purposes. Add a license file (for example MIT) if you want others to reuse it.

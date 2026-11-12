# ImageRecognizer — تطبيق التعرف على الصور

تطبيق أندرويد (Kotlin + Jetpack Compose) يتعرف على محتوى الصورة **على الجهاز وبدون إنترنت** باستخدام Google ML Kit.

## المزايا
- اختيار صورة من المعرض أو التقاطها بالكاميرا
- عرض أفضل التصنيفات مع نسبة الثقة
- بنية نظيفة: `data` (النموذج) / `ui` (الواجهة + ViewModel)

## الهيكل
```
app/src/main/kotlin/com/example/mycompose/imagerecognizer/
├── MainActivity.kt
├── data/
│   ├── ImageClassifier.kt      ← غلاف ML Kit (غيّره لاستخدام نموذج TFLite مخصص)
│   └── Prediction.kt
└── ui/
    ├── HomeScreen.kt
    ├── RecognitionViewModel.kt
    └── theme/ (Color, Theme, Type)
```

## البناء
- محلياً: `./gradlew assembleDebug` (أضف gradle wrapper أو ولّده من IDE)
- على GitHub: كل `push` يشغّل Actions ويرفع ملف `app-debug.apk` ضمن Artifacts.

## الرفع إلى GitHub
```
git init && git add . && git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<USER>/ImageRecognizer.git
git push -u origin main
```

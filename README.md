# NutriFit — AI-нутрициолог

Android-приложение для распознавания блюда по фотографии и расчёта калорийности с помощью нейросети. Курсовой проект по модулю «Прикладные задачи машинного и глубокого обучения».

## ✨ Функции

- 📸 Распознавание 101 класса еды (датасет Food-101) через камеру или галерею
- ⚖️ Ползунок для выбора веса порции (граммы)
- 🔥 Автоматический расчёт калорий на основе базы данных
- 🎨 Material Design, карточки, адаптивный интерфейс

## 🛠 Технологии

- **Языки:** Kotlin, Java
- **Android SDK:** min 24, target 33
- **Машинное обучение:** TensorFlow Lite, модель EfficientNet-B0 (обучена на PyTorch, конвертирована в TFLite)
- **Камера:** CameraX
- **Загрузка изображений:** Glide
- **UI:** Material Design Components, ConstraintLayout, SeekBar, MaterialCardView
 ```mermaid
   flowchart TD
    A[Пользователь запускает приложение] --> B{Выбор источника}
    B -->|Камера| C[Сделать фото]
    B -->|Галерея| D[Выбрать фото из памяти]
    C --> E[Получение Bitmap]
    D --> E
    E --> F[TensorFlow Lite Interpreter]
    F --> G[Модель EfficientNet-B0]
    G --> H[Распознанный класс + confidence]
    H --> I[Отображение: название блюда (xx%)]
    I --> J[Пользователь выбирает вес<br>(SeekBar)]
    J --> K[База калорий (HashMap)]
    K --> L[Расчёт калорий: вес * ккал/100]
    L --> M[Отображение результата]
``` 

## 📥 Установка и запуск

1. Установите приложение на телефон с Android 7+.
2. **Важно:** Модель `food_classifier_float32.tflite` и файл меток `labels.txt` НЕ входят в репозиторий из-за большого размера. Скачайте их по ссылке: 
   [https://drive.google.com/drive/folders/1wKh24Te_4Bgy084CvmErIim_Ri6dh_SJ?usp=sharing]() .
3. Поместите оба файла в папку `app/src/main/assets/` вашего проекта.
4. Соберите и запустите проект в Android Studio.

## 🧠 Обучение модели

- **Архитектура:** EfficientNet‑B0 (предобучена на ImageNet)
- **Датасет:** Food‑101 (101 000 изображений, 101 класс)
- **Фреймворк:** PyTorch
- **Аугментация:** RandomHorizontalFlip, RandomRotation, ColorJitter
- **Точность на валидации:** 80.8%

## 🗂 Структура проекта
NutriFit/
├── app/src/main/
│ ├── assets/ # здесь лежат .tflite и labels.txt (игнорируются Git)
│ ├── java/.../nutrifit/
│ │ ├── FoodClassifier.java # загрузка модели и инференс
│ │ └── MainActivity.java # логика камеры, галереи, расчёта калорий
│ ├── res/ # ресурсы: layout, drawable, xml
│ └── AndroidManifest.xml
├── build.gradle # настройки сборки проекта
└── README.md # этот файл

## 📄 Лицензия

MIT License. Проект выполнен в учебных целях.

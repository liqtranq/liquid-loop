<div align="center">

# Liquid Converter

**Специализированный аудиоплеер и инструмент для зацикливания отрезков.**

Интерактивная волна · Бесшовный лупинг · Микротюнинг · Фоновый режим

![release](https://img.shields.io/github/v/release/liqtranq/liquid-converter?label=release&color=orange)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84.svg?logo=android)
![License](https://img.shields.io/badge/License-MIT-blue)

[Скачать APK](https://github.com/liqtranq/liquid-converter/releases) · [Сообщить об ошибке](https://github.com/liqtranq/liquid-converter/issues)

</div>

## О проекте

Liquid Converter — это аудиоплеер для музыкантов, поэтов, сонграйтеров и писателей с расширенными возможностями фоновой работы. Слушайте музыку, выбирайте точный фрагмент (луп A-B) и оставляйте его играть по кругу без пауз, пока пишете текст в других приложениях.

В оформлении — тёмные поверхности (Liquid UI), глубокие фиолетовые и неоново-синие акценты, создающие стильное пространство для творчества и фокуса.

## Возможности

| | Что умеет приложение |
| --- | --- |
| **Лупинг** | Бесшовное зацикливание A-B фрагментов аудио без щелчков и пауз |
| **Управление** | Интерактивная волна (Waveform) для визуального выбора отрезков и микротюнинг (±50мс) для идеальной точности |
| **Фоновый режим** | Воспроизведение в фоне, а также плавающий виджет Picture-in-Picture (PiP) для управления поверх других окон |
| **Аудиодвижок** | Высокоточный движок на базе AndroidX Media3 (ExoPlayer) с кастомным LoopWatcher |

## Установка

Нужен Android 8.0 или новее. Откройте [релизы](https://github.com/liqtranq/liquid-converter/releases), выберите версию и скачайте APK. Файлы с `debug` в названии — тестовые сборки.

Сборки Liquid Converter публикуются в этом репозитории.

## Сборка из исходников

Требуются JDK 17+ и Android SDK 37. Укажите путь к SDK в `local.properties` или через `ANDROID_HOME`.

```bash
git clone https://github.com/liqtranq/liquid-converter.git
cd liquid-converter
./gradlew :app:assembleDebug
```

На Windows используйте `./gradlew.bat`. APK появится в `app/build/outputs/apk/debug/`.

## Лицензия

[MIT License](https://github.com/liqtranq/liquid-converter/blob/main/LICENSE). 

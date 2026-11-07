#!/usr/bin/env bash
set -e

echo "=========================================================="
echo " Сборка Hamster Wheel Glyph Toy (Nothing Phone 4a Pro)    "
echo " Матрица: 13x13 (137 mini-LEDs, Glyph.DEVICE_25111p)      "
echo "=========================================================="

mkdir -p app/libs
if [ ! -f "app/libs/glyph-matrix-sdk-2.0.aar" ]; then
    echo "[1/3] Скачивание официального Nothing Glyph Matrix SDK 2.0..."
    curl -L "https://raw.githubusercontent.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/main/glyph-matrix-sdk-2.0.aar"          -o "app/libs/glyph-matrix-sdk-2.0.aar"
else
    echo "[1/3] Файл glyph-matrix-sdk-2.0.aar уже находится в app/libs/"
fi

echo "[2/3] Запуск сборки APK..."
if [ -x "./gradlew" ]; then
    ./gradlew assembleDebug
elif command -v gradle >/dev/null 2>&1; then
    gradle assembleDebug
else
    echo "Ошибка: не найден gradle или ./gradlew."
    echo "В Termux установите: pkg install openjdk-17 gradle android-tools wget"
    exit 1
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
echo "[3/3] Готово! Ваш установочный файл собран:"
echo "      $APK_PATH"

if [ -d "/sdcard/Download" ]; then
    cp "$APK_PATH" "/sdcard/Download/HamsterWheel-Nothing4aPro.apk"
    echo "--> Копия сохранена в Загрузки: /sdcard/Download/HamsterWheel-Nothing4aPro.apk"
    echo "    Откройте приложение 'Файлы' на телефоне и нажмите на APK для установки!"
fi

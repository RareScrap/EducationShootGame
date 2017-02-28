package ru.rarescrap.educationshootgame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CannonView"; // Для регистрации ошибок

    // Игровые константы
    public static final int MISS_PENALTY = 2; // Штраф при промахе
    public static final int HIT_REWARD = 3; // Прибавка при попадании

    // Константы для рисования пушки
    public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;

    // Константы для рисования ядра
    public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
    public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;

    // Константы для рисования мишеней
    public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
    public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
    public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
    public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
    public static final double TARGET_PIECES = 9; // TODO: Протестить при большем количестве мишеней
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 4;

    // Константы для рисования блока
    public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
    public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    public static final double BLOCKER_X_PERCENT = 1.0 / 2;
    public static final double BLOCKER_SPEED_PERCENT = 1.0;

    // Размер текста составляет 1/18 ширины экрана
    public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

    private CannonThread cannonThread; // Управляет циклом игры
    private Activity activity; // Для отображения окна в потоке GUI
    private boolean dialogIsDisplayed = false;

    // Игровые объекты
    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets;

    // Переменные размеров ( обновляются в onSizeChanged() )
    private int screenWidth;
    private int screenHeight;

    // Переменные для игрового цикла и отслеживания состояния игры
    private boolean gameOver; // Игра закончена?
    private double timeLeft; // Оставшееся время в секундах
    private int shotsFired; // Количество сделанных выстрелов
    private double totalElapsedTime; // Затраты времени в секундах

    // Константы и переменные для управления звуком
    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // Воспроизведение звуков
    private SparseIntArray soundMap; // Связь идентификаторов с SoundPool

    // Переменные Paint для рисования элементов на экране
    private Paint textPaint; // Для вывода текста
    private Paint backgroundPaint; // Для стирания области рисования


    // Конструктор
    public CannonView(Context context, AttributeSet attrs) { // TODO: Зачем тут AttributeSet?
        super(context, attrs); // Вызов конструктора суперкласса
        activity = (Activity) context; // Ссылка на MainActivity

        // Регистрация слушателя SurfaceHolder.Callback
        getHolder().addCallback(this);

        // Настройка атрибутов для воспроизведения звука
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

        // Инициализация SoundPool для воспроизведения звука
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        builder.setAudioAttributes(attrBuilder.build()); // Связыванеие атрибутов с soundPool
        soundPool = builder.build();

        // Создание Map и предварительная загрузка звуков
        soundMap = new SparseIntArray(3); // Создание SparseIntArray (как HashMap, о более эфективный для небльшого количство пар)
        soundMap.put(TARGET_SOUND_ID,
                soundPool.load(context, R.raw.target_hit, 1)); // Возвращает int итендификатор загруженного звука
        soundMap.put(CANNON_SOUND_ID,
                soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID,
                soundPool.load(context, R.raw.blocker_hit, 1));

        textPaint = new Paint();
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
    }

    // Вызывается при изменении размера SurfaceView,
    // например при первом добавлении в иерархию View
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w; // Сохранение ширины CannonView
        screenHeight = h; // Сохранение высоты CannonView

        // Настройка свойств текста
        textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
        textPaint.setAntiAlias(true); // Сглаживание текста
    }

    // Получение ширины экрана
    public int getScreenWidth() {
        return screenWidth;
    }

    // Получение высоты экрана
    public int getScreenHeight() {
        return screenHeight;
    }

    // Воспроизведение звука с заданным идентификатором soundId в soundMap
    public void playSound(int soundId) {
        soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f); // TODO: Что такое 1f? 1.0? Но зачем?
    }

    // Сброс всех экранных элементов и запуск новой игры
    public void newGame() {
        // Создание новой пушки
        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

        Random random = new Random(); // Для случайных скоростей
        targets = new ArrayList<>(); // Построение нового списка мишеней

        // Инициализация targetX для первой мишени слева
        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);

        // Вычисление координаты Y
        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) * screenHeight);

        // Добавление TARGET_PIECES мишеней в список
        for (int n = 0; n < TARGET_PIECES; n++) {
            // Получение случайной скорости в диапазоне от min до max
            // для мишени n
            double velocity = screenHeight * (random.nextDouble() *
                    (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) +
                    TARGET_MIN_SPEED_PERCENT);

            // Цвета мишеней чередуются между белым и черным
            int color = (n % 2 == 0)
                    ?
                    getResources().getColor(R.color.dark, getContext().getTheme())
                    :
                    getResources().getColor(R.color.light, getContext().getTheme());

            // TODO: Разме переменная velocity не обнуляется за каждую итерацию
            velocity *= -1; // Противоположная скорость следующей мишени

            // Создание и добавление новой мишени в список
            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    (int) (TARGET_WIDTH_PERCENT * screenWidth),
                    (int) (TARGET_LENGTH_PERCENT * screenHeight),
                    (int) velocity));

            // Увеличение координаты x для смещения
            // следующей мишени вправо
            targetX += (TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) * screenWidth;
        }

        // Создание нового блока
        blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (BLOCKER_SPEED_PERCENT * screenHeight));

        timeLeft = 10; // Обратный отсчет с 10 секунд

        shotsFired = 0; // Начальное количество выстрелов
        totalElapsedTime = 0.0; // Обнулить затраченное время

        // TODO: ХЗ как это работает
        if (gameOver) {// Начать новую игру после завершения предыдущей
            gameOver = false; // Игра не закончена
            cannonThread = new CannonThread(getHolder()); // Создать поток
            cannonThread.start(); // Запуск потока игрового цикла
        }

        hideSystemBars();
    }

    // Многократно вызывается CannonThread для обновления элементов игры
    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0; // Преобразовать в секунды

        // Обновление позиции ядра
        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);

        blocker.update(interval); // Обновление позиции блока

        for (GameElement target : targets)
            target.update(interval); // Обновление позиции мишени

        timeLeft -= interval; // Уменьшение оставшегося времени

        // Если счетчик достиг нуля
        if (timeLeft <= 0) {
            timeLeft = 0.0;
            gameOver = true; // Игра закончена
            cannonThread.setRunning(false); // Завершение потока
            showGameOverDialog(R.string.lose); // Сообщение о проигрыше
        }

        // Если все мишени поражены
        if (targets.isEmpty()) {
            cannonThread.setRunning(false); // Завершение потока
            showGameOverDialog(R.string.win); // Сообщение о выигрыше
            gameOver = true;
        }
    }
}

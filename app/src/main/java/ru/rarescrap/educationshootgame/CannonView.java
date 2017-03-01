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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes.Builder attrBuilder = null;
            attrBuilder = new AudioAttributes.Builder();
            attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

            // Инициализация SoundPool для воспроизведения звука
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(1);
            builder.setAudioAttributes(attrBuilder.build()); // Связыванеие атрибутов с soundPool
            soundPool = builder.build();
        }

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
            int color;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                color = (n % 2 == 0)
                        ?
                        getResources().getColor(R.color.dark, getContext().getTheme())
                        :
                        getResources().getColor(R.color.light, getContext().getTheme());
            }else {
                color = (n % 2 == 0)
                        ?
                        getResources().getColor(R.color.dark)
                        :
                        getResources().getColor(R.color.light);
            }

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
            timeLeft = 0.0; // если этого не сделать, на экране может отобразиться отрицательное значение времени
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

    // Метод определяет угол наклона ствола и стреляет из пушки,
    // если ядро не находится на экране
    public void alignAndFireCannonball(MotionEvent event) {
        // Получение точки касания в этом представлении
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());

        // Вычисление расстояния точки касания от центра экрана
        // по оси y
        double centerMinusY = (screenHeight / 2 - touchPoint.y); // TODO: Как это блять работает?

        double angle = 0; // Инициализировать angle значением 0

        // Вычислить угол ствола относительно горизонтали
        angle = Math.atan2(touchPoint.x, centerMinusY);

        // Ствол наводится в точку касания
        cannon.align(angle);

        // Пушка стреляет, если ядро не находится на экране
        if (cannon.getCannonball() == null || !cannon.getCannonball().isOnScreen()) {
            cannon.fireCannonball();
            ++shotsFired;
        }
    }

    // Отображение окна AlertDialog при завершении игры
    private void showGameOverDialog(final int messageId) {
        // Объект DialogFragment для вывода статистики и начала новой игры
        final DialogFragment gameResult = new DialogFragment() { // TODO: Исправить на статик и разобраться
            // Метод создает объект AlertDialog и возвращает его
            @Override
            public Dialog onCreateDialog(Bundle bundle) {
                // Создание диалогового окна с выводом строки messageId
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(messageId));

                // Вывод количества выстрелов и затраченного времени
                builder.setMessage(getResources().getString(R.string.results_format, shotsFired, totalElapsedTime));
                builder.setPositiveButton(
                    R.string.reset_game,
                    new DialogInterface.OnClickListener() {
                        // Вызывается при нажатии кнопки "Reset Game"
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialogIsDisplayed = false;
                            newGame(); // Создание и начало новой партии
                        }
                    }
                ); // end of setPositiveButton()

                return builder.create(); // Вернуть AlertDialog
            }
        };

        // В UI-потоке FragmentManager используется для вывода DialogFragment
        activity.runOnUiThread( // TODO: Проверить, выводится ли апп из режима погружения когда показывается диалог
            new Runnable() {
                public void run() {
                    showSystemBars(); // Выход из режима погружения
                    dialogIsDisplayed = true;
                    gameResult.setCancelable(false); // Модальное окно
                    gameResult.show(activity.getFragmentManager(), "results");
                }
            }
        );
    }

    // Рисование элементов игры
    public void drawGameElements(Canvas canvas) { // TODO: Откуда берется Canvas?
        // Очистка фона
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        // Вывод оставшегося времени
        // TODO: Вычислить математически
        canvas.drawText(getResources().getString(R.string.time_remaining_format, timeLeft), 50, 100, textPaint); // TODO: Почему точка, где начианет рисоватся текст, прибита?

        cannon.draw(canvas); // Рисование пушки (и ствол, и основание)

        // Рисование игровых элементов
        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);

        blocker.draw(canvas); // Рисование блока

        // Рисование всех мишеней
        for (GameElement target : targets)
            target.draw(canvas);
    }

    // Проверка столкновений с блоком или мишенями и обработка столкновений
    public void testForCollisions() {
        // Удаление мишеней, с которыми сталкивается ядро
        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen()) {
            for (int n = 0; n < targets.size(); n++) {
                if (cannon.getCannonball().collidesWith(targets.get(n))) {
                    targets.get(n).playSound(); // Звук попадания в мишень

                    // Прибавление награды к оставшемуся времени
                    timeLeft += targets.get(n).getHitReward();

                    cannon.removeCannonball(); // Удаление ядра из игры
                    targets.remove(n); // Удаление пораженной мишени
                    --n; // Чтобы не пропустить проверку новой мишени
                    break;
                }
            }
        }else { // Удаление ядра, если оно не должно находиться на экране
            cannon.removeCannonball();
        } // end of if

        // Проверка столкновения с блоком
        if (cannon.getCannonball() != null && cannon.getCannonball().collidesWith(blocker)) {
            blocker.playSound(); // play Blocker hit sound

            // Изменение направления
            cannon.getCannonball().reverseVelocityX(); // TODO: Зачем это делать?

            // Уменьшение оставшегося времени на величину штрафа
            timeLeft -= blocker.getMissPenalty();
        }
    }

    // Остановка игры; вызывается методом onPause класса MainActivityFragment
    public void stopGame() { // TODO: Прогресс тоже сбросиьтся?
        if (cannonThread != null)
            cannonThread.setRunning(false); // Приказываем потоку завершиться
    }

    // Освобождение ресурсов; вызывается методом onDestroy класса CannonGame
    public void releaseResources() {
        soundPool.release(); // Освободить все ресурсы, используемые SoundPool
        soundPool = null;
    }



    // Реализация методов SurfaceHolder.Callback
    // Вызывается при изменении размера поверхности
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    // Вызывается при создании поверхности - при первой загрузке приложения или при возвращении его из фонового режима.
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            newGame(); // Создание новой игры

            cannonThread = new CannonThread(holder); // Создание потока
            cannonThread.setRunning(true); // Запуск игры
            cannonThread.start(); // Запуск потока игрового цикла
        }
    }

    // Вызывается при уничтожении поверхности - например, при завершении приложения
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Обеспечить корректную зависимость потока
        boolean retry = true;
        cannonThread.setRunning(false); // Завершение cannonThread

        // TODO: Пройти пошагово
        while (retry) {
            try {
                cannonThread.join(); // Ожидать завершения cannonThread
                retry = false;
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted", e);
            }
        }
    }

    // Вызывается при касании экрана в этой активности
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // int представляет тип действия, вызвавшего данное событие (такие как ACTION_DOWN и т.д.)
        int action = e.getAction();

        // Пользователь коснулся экрана или провел пальцем по экрану
        // Тут событие обрабатыватся ТОЛЬКО ДЛЯ ОДНОГО пальца
        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE) {
            // Выстрел в направлении точки касания
            alignAndFireCannonball(e);
        }

        return true;
    }



    // Субкласс Thread для управления циклом игры
    private class CannonThread extends Thread {
        private SurfaceHolder surfaceHolder; // Для работы с Canvas
        private boolean threadIsRunning = true; // По умолчанию

        // Инициализация SurfaceHolder
        public CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder; // TODO: Зачем ужен холдер?
            setName("CannonThread");
        }

        // Изменение состояния выполнения
        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        // Управление игровым циклом (покадровыми аимациями)
        // Каждое обновление элементов игры на экране выполняется с учетом количества миллисекунд, прошедших с момента последнего обновления
        @Override
        public void run() {
            Canvas canvas = null; // Используется для рисования
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    // Получение Canvas для монопольного рисования из этого потока
                    canvas = surfaceHolder.lockCanvas(null); // TODO: Почему бы не юзать lockCanvas() без аргов?

                    // Блокировка surfaceHolder для рисования
                    synchronized(surfaceHolder) { // TODO: Что такое synchronized с точки зрения языка?
                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsedTime += elapsedTimeMS / 1000.0;
                        updatePositions(elapsedTimeMS); // Обновление состояния игры
                        testForCollisions(); // Проверка столкновений с GameElement
                        drawGameElements(canvas); // Рисование на объекте canvas
                        previousFrameTime = currentTime; // Обновление времени
                    }
                }
                finally { // Ключевое слово finally создаёт блок кода, который будет выполнен после завершения блока try/catch, но перед кодом, следующим за ним
                    if (canvas != null) // Вывести содержимое canvas на CannonView и разрешить использовать Canvas другим потокам
                        surfaceHolder.unlockCanvasAndPost(canvas); // TODO: Что будет, если передать внутрь канват отличный от lockCanvas()?
                }
            }
        }
    }

    // Режим погружения доступен только на устройствах с Android 4.4 и выше
    // Сокрытие системных панелей и панели приложения
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // Вывести системные панели и панель приложения
    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | // TODO: Зачем тут побитовое ИЛИ?
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                /*  С этими константами View приложение не будет изменяться в размерах при
                    каждом сокрытии и появлении системных панелей и панели приложения.
                    Вместо этого системные панели и панель приложения будут накладываться на CannonView
                 */ // TODO: Проверить
    }
}

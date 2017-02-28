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
    public static final double TARGET_PIECES = 9;
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

    // Переменные размеров
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
}

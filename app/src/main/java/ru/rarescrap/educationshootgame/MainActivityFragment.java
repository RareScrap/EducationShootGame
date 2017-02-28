// MainActivityFragment.java
// Класс MainActivityFragment создает и управляет CannonView
package ru.rarescrap.educationshootgame;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.rarescrap.educationshootgame.CannonView;

public class MainActivityFragment extends Fragment {
    private CannonView cannonView; // Пользовательское представление для игры

    // Вызывается при создании представления фрагмента
    // @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Заполнение макета fragment_main.xml
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Получение ссылки на CannonView
        cannonView = (CannonView) view.findViewById(R.id.cannonView);
        return view;
    }

    // Настройка управления громкостью при создании управляющей активности
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Разрешить использование кнопок управления громкостью
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    // При приостановке MainActivity игра завершается
    @Override
    public void onPause() {
        super.onPause();
        cannonView.stopGame(); // Завершение игры
    }

    // При приостановке MainActivity освобождаются ресурсы
    @Override
    public void onDestroy() {
        super.onDestroy();
        cannonView.releaseResources(); // Освобождает звуковые ресурсы
    }
}
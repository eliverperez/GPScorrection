package famisa.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Eliver on 30/06/18.
 */

public class UnsilenceBroadcastReceiver extends BroadcastReceiver {

    MainActivity mainActivity;

    @Override
    public void onReceive(Context context, Intent intent) {
        mainActivity = (MainActivity) context;
//        AudioManager audio = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
//
//        audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }
}

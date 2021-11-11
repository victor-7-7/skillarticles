package ru.skillbranch.skillarticles.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract

class EditImageContract : ActivityResultContract<Pair<Uri, Uri>, Uri?>() {

    override fun createIntent(context: Context, input: Pair<Uri, Uri>): Intent {
        // Говорим системе, что желаем редактировать
        // [action=>Intent.ACTION_EDIT] данные типа "image/jpeg".
        // Система предложит юзеру выбрать редактор изображений и
        // затем откроет в редакторе файл по uri => input!!.first
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(input!!.first, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // В экстру кладем второй uri, по которому будем
            // сохранять отредактированный файл изображения
            putExtra(MediaStore.EXTRA_OUTPUT, input.second)
            // lecture 12, t.c. 01:41:21
            putExtra("return-value", true)
        }
        // Надо выдать разрешение на запись в указанный uri, но не всем
        // установленным на девайсе приложениям, а только тем, которые
        // могут редактировать изображения (с категорией активити default).
        // Lecture 12, 01:49:07
        val grantedApps = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { info -> info.activityInfo.packageName }
        // Отрываем forEach от цепочки, чтобы можно было grantedApps вывести в лог
        grantedApps.forEach { pack ->
            context.grantUriPermission(
                pack, input!!.second, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        Log.d(
            "M_S_EditImageContract", "createIntent: activities (apps) " +
                    "for edit image: $grantedApps"
        )
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data
        else null
    }
}
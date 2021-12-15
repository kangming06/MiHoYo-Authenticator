package hat.auth.activities.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.skydoves.landscapist.coil.CoilImage
import hat.auth.R
import hat.auth.activities.MainActivity
import hat.auth.data.DailyNote
import hat.auth.data.GameRecord
import hat.auth.data.JourneyNotes
import hat.auth.data.MiAccount
import hat.auth.utils.MiHoYoAPI
import hat.auth.utils.getDrawableAsImageBitmap
import kotlinx.coroutines.delay

private val unknownAvatar by lazy {
    getDrawableAsImageBitmap(R.drawable.ic_unknown)
}

private var currentDailyNote    by mutableStateOf(DailyNote())
private var currentGameRecord   by mutableStateOf(GameRecord())
private var currentJourneyNotes by mutableStateOf(JourneyNotes())

private var isDialogShowing by mutableStateOf(false)

fun showInfoDialog(
    note: DailyNote,
    record: GameRecord,
    journeyNotes: JourneyNotes
) {
    currentDailyNote    = note
    currentGameRecord   = record
    currentJourneyNotes = journeyNotes
    isDialogShowing = true
}

@Composable
fun MainActivity.InfoDialog() {
    if (isDialogShowing) IND()
}

private fun hm(i: Int)  = "%02d:%02d".format(i / 3600, (i % 3600) / 60)
private fun hms(i: Int) = "(%02d:%02d:%02d)".format(i / 3600, (i % 3600) / 60, i % 60)

@Composable
@Suppress("unused")
private fun MainActivity.IND() = Dialog(
    onDismissRequest = {
        isDialogShowing = false
    }
) {
    var resinRecTime = currentDailyNote.resinRecoveryTime.toInt()
    var remaining by remember { mutableStateOf(hms(resinRecTime)) }
    Column(
        modifier = Modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            with(currentDailyNote) {
                Oculi(
                    resId = R.drawable.ic_resin,
                    text = "${currentResin}/${maxResin} $remaining",
                    horizontalSpacedBy = 5.dp,
                    size = 24.dp
                )
                Oculi(
                    resId = R.drawable.ic_commission,
                    text = "${finishedTaskNum}/${totalTaskNum}",
                    horizontalSpacedBy = 5.dp,
                    size = 24.dp
                )
            }
            LaunchedEffect(Unit) {
                while (isDialogShowing) {
                    resinRecTime --
                    remaining = if (resinRecTime >= 0) hms(resinRecTime) else ""
                    delay(1000)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            with(currentJourneyNotes.monthData) {
                Oculi(
                    resId = R.drawable.ic_primogem,
                    text = if (currentPrimogems == primogemsRate * 100) {
                        "$currentPrimogems"
                    } else {
                        "$currentPrimogems (${primogemsRate}%)"
                    },
                    horizontalSpacedBy = 5.dp,
                    size = 24.dp
                )
                Oculi(
                    resId = R.drawable.ic_mora,
                    text = if (currentMora == moraRate * 100) {
                        "$currentMora"
                    } else {
                        "$currentMora (${moraRate}%)"
                    },
                    horizontalSpacedBy = 5.dp,
                    size = 24.dp
                )
            }
        }
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset((-5).dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            for (i in 0 until currentDailyNote.maxExpeditionNum) {
                val e = currentDailyNote.expeditions.getOrElse(i) {
                    DailyNote.Expedition("unknown","-1")
                }
                val u = "${e.avatarSideIcon}?x-oss-process=image/resize,p_100/crop,x_20,y_33,w_95,h_95"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rt = e.remainedTime.toInt()
                    Box {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.BottomCenter)
                                .border(
                                    width = 2.dp,
                                    color = when (rt) {
                                        0 -> Color(0xFF84BD1F)
                                        -1 -> Color(0xFFC4C4C4)
                                        else -> Color(0xFFDC9F51)
                                    },
                                    shape = CircleShape
                                )
                        )
                        CoilImage(
                            imageModel = u,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.TopCenter),
                            placeHolder = unknownAvatar,
                            contentScale = ContentScale.Inside
                        )
                    }
                    Text(
                        text = when(rt) {
                            0 -> "已完成"
                            -1 -> "未派遣"
                            else -> hm(rt)
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }
        Divider()
        with(currentGameRecord.stats) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Oculi(
                    resId = R.drawable.ic_anemoculus,
                    text = "${anemoculusNumber}/66"
                )
                Oculi(
                    resId = R.drawable.ic_geoculus,
                    text = "${geoculusNumber}/131"
                )
                Oculi(
                    resId = R.drawable.ic_electroculus,
                    text = "${electroculusNumber}/181"
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Chest(
                    resId = R.drawable.ic_common_chest,
                    num = commonChestNumber
                )
                Chest(
                    resId = R.drawable.ic_exquisite_chest,
                    num = exquisiteChestNumber
                )
                Chest(
                    resId = R.drawable.ic_precious_chest,
                    num = preciousChestNumber
                )
                Chest(
                    resId = R.drawable.ic_luxurious_chest,
                    num = luxuriousChestNumber
                )
                Chest(
                    resId = R.drawable.ic_magic_chest,
                    num = magicChestNumber
                )
            }
        }
        LaunchedEffect(Unit) {
            var first = true
            while (isDialogShowing) {
                if (!first) {
                    runCatching {
                        currentDailyNote = MiHoYoAPI.getDailyNote(currentAccount as MiAccount)
                        resinRecTime = currentDailyNote.resinRecoveryTime.toInt()
                    }
                } else {
                    first = false
                }
                delay(60000)
            }
        }
    }
}

@Composable
private fun Chest(
    @DrawableRes resId: Int,
    num: Int
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        modifier = Modifier.size(28.dp)
    )
    Text(
        text = num.toString(),
        fontSize = 14.sp
    )
}

@Composable
private fun Oculi(
    @DrawableRes resId: Int,
    text: String,
    size: Dp = 28.dp,
    horizontalSpacedBy: Dp = 0.dp
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(horizontalSpacedBy)
) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        modifier = Modifier.size(size)
    )
    Text(text)
}

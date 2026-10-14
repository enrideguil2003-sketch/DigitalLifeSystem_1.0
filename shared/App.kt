package com.digitallifesystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitallifesystem.shared.resources.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.graphics.PathEffect
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import org.jetbrains.compose.resources.painterResource

@Composable
private fun blenderBook(): FontFamily = FontFamily(
    Font(Res.font.BlenderPro_Book, FontWeight.Normal),
    Font(Res.font.BlenderPro_BookItalic, FontWeight.Normal)
)

@Composable
private fun blenderBold(): FontFamily = FontFamily(
    Font(Res.font.BlenderPro_Bold, FontWeight.Bold)
)
private val Green = Color(0xFF04D204); private val Light = Color(0xFF79FF79)
private val DarkBlue = Color(0xFF0A2340)
private val Grid = Color(0xFF126512); private val Red = Color(0xFFFF3333); private val White=Color.White
private val Black=Color.Black; private val Panel=Color(0xCC050B10)

@Composable fun App() {
    val vm = remember { LifeViewModel() }
    var startupComplete by remember { mutableStateOf(false) }
    var logged by remember { mutableStateOf(false) }

    // Keep file parsing, legacy reset/migration and session lookup off the UI
    // thread. A large users.json must not keep Android's splash/loading screen
    // visible indefinitely while the main thread is blocked on disk I/O.
    LaunchedEffect(vm) {
        val startup = try {
            withContext(Dispatchers.IO) {
                PlatformStorage.initialize()
                PlatformStorage.loadUsers() to PlatformStorage.loadSessionUsername()
            }
        } catch (_: Exception) {
            mutableMapOf<String, UserData>() to null
        }
        try { vm.loadExerciseCatalog(Res.readBytes("files/exercises.txt").decodeToString()) } catch (_: Throwable) { }
        logged = try {
            vm.initialize(startup.first, startup.second)
        } catch (_: Exception) {
            // Never leave the app on the loading screen if a legacy profile
            // contains malformed optional data; allow the login screen to open.
            false
        }
        startupComplete = true
    }

    if (!startupComplete) {
        MainLoadingScreen()
    } else if (!logged) {
        LoginScreen { user, pass ->
            val ok = vm.login(user, pass)
            if (ok) logged = true
            ok
        }
    } else {
        MainScreen(vm) { logged = false }
    }
}

@Composable private fun MainLoadingScreen(){
    Box(Modifier.fillMaxSize().background(Black), contentAlignment=Alignment.Center){
        Text("Загрузка...", color=Light, fontFamily=blenderBold(), fontSize=16.sp)
    }
}

@Composable private fun LoginScreen(onLogin:(String,String)->Boolean) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    fun submit() {
        val login = user.trim()
        val password = pass
        if (login.isBlank() || password.isBlank()) {
            error = "Введите логин и пароль"
            return
        }
        if (onLogin(login, password)) error = ""
        else error = "Неверный логин или пароль"
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Black)) {
        val isPhone = this.maxWidth < 700.dp
        Image(painterResource(Res.drawable.fon), null, Modifier.fillMaxSize(), contentScale=ContentScale.Crop)
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().heightIn(min = screenHeight)) {
        Image(
            painterResource(Res.drawable.logo),
            null,
            Modifier.size(if (isPhone) 108.dp else 122.dp)
                .align(Alignment.TopCenter)
                .padding(top = if (isPhone) 28.dp else 36.dp)
        )
        val panelWidth = if (isPhone) (screenWidth - 28.dp).coerceAtLeast(300.dp) else 420.dp
        val panelHeight = if (isPhone) 410.dp else 440.dp
        Spacer(Modifier.height(panelHeight + 300.dp))
        Surface(
            Modifier.width(panelWidth).height(panelHeight).align(Alignment.Center),
            color=Black,
            border=BorderStroke(6.dp,Green)
        ) {
            Column(
                Modifier.padding(horizontal = if (isPhone) 30.dp else 42.dp, vertical = 34.dp),
                horizontalAlignment=Alignment.CenterHorizontally
            ) {
                Text("Authorization", color=White, fontFamily=blenderBold(), fontSize=21.sp, modifier=Modifier.padding(bottom=25.dp))
                OutlinedTextField(
                    value=user,
                    onValueChange={user=it},
                    label={Text("Login")},
                    singleLine=true,
                    keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next),
                    modifier=Modifier.fillMaxWidth(),
                    colors=darkFields()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value=pass,
                    onValueChange={pass=it},
                    label={Text("Password")},
                    singleLine=true,
                    visualTransformation=PasswordVisualTransformation(),
                    keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),
                    keyboardActions=KeyboardActions(onDone={submit()}),
                    modifier=Modifier.fillMaxWidth(),
                    colors=darkFields()
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick={submit()},
                    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF050805),contentColor=White),
                    border=BorderStroke(1.dp,Color.Gray),
                    shape=RoundedCornerShape(24.dp),
                    modifier=Modifier.width(if (isPhone) 250.dp else 280.dp).height(43.dp)
                ) { Text("Log in",fontFamily=blenderBold(),fontSize=14.sp) }
                if(error.isNotEmpty()) Text(error,color=Red,fontFamily=blenderBook(),fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
            }
        }
        }
        }
    }
}

@Composable private fun darkFields()=OutlinedTextFieldDefaults.colors(focusedTextColor=White,unfocusedTextColor=White,focusedBorderColor=Green,unfocusedBorderColor=Color(0xFF555555),focusedLabelColor=Light,unfocusedLabelColor=Color.Gray,cursorColor=Green)

private enum class EditorMode { CREATE, EDIT }
private data class DialogState(val title:String,val text:String="",val onYes:()->Unit={})

private fun points(difficulty: String): Int = when (difficulty) {
    "Легкий" -> 10
    "Средний" -> 25
    "Тяжелый" -> 50
    "Экстремальный" -> 100
    else -> 10
}

private data class TrainingExercise(val name:String,val prescription:String,val difficulty:String,val description:String="",val key:String="",val isCustom:Boolean=false)

private fun trainingDescriptionFor(name:String):String = when {
    name.contains("Отжимания от стены") -> "Упритесь руками в стену, держите корпус ровно и плавно согните локти, затем вернитесь в исходное положение."
    name.contains("Отжимания от опоры") -> "Опирайтесь руками на устойчивую поверхность, держите корпус в одну линию и опускайтесь плавно."
    name.contains("Отжимания с колен") -> "Колени на полу, корпус прямой, опускайте грудь к полу и поднимайтесь без рывка."
    name.contains("Отжимания") -> "Ладони чуть шире плеч, корпус прямой, опускайтесь контролируемо и выжимайте себя вверх."
    name.contains("Жим лёгкой гири лёжа") -> "Лягте на спину, удерживайте гирю у груди и выжимайте вверх, не выгибая поясницу."
    name.contains("Жим лёгких гантелей лёжа") -> "Лягте на скамью, гантели у груди, выжмите вверх и медленно опустите."
    name.contains("Жим гантелей лёжа") -> "Стабилизируйте лопатки, держите гантели у груди и выжимайте их вверх по контролируемой траектории."
    name.contains("Жим гантелей на наклонной скамье") -> "Скамья под умеренным наклоном, лопатки сведены; опускайте гантели к верхней части груди и выжимайте вверх."
    name.contains("Жим штанги лёжа") -> "Стопы на полу, лопатки сведены, штангу опускайте к нижней части груди и выжимайте вверх без рывка."
    name.contains("Сведение рук") || name.contains("Сведения рук") -> "Слегка согните локти и плавно сводите руки перед собой, сохраняя напряжение груди."
    name.contains("Сведения на блоках") -> "Держите корпус устойчиво и сведите рукояти по дуге перед собой, не бросая вес."
    name.contains("Медленные отжимания") -> "Опускайтесь медленно на 2–3 секунды, сохраняйте прямую линию корпуса и поднимайтесь спокойно."
    name.contains("Тяга резинки сидя") -> "Сядьте ровно, зафиксируйте корпус и тяните резинку к поясу, сводя лопатки."
    name.contains("Тяга лёгкой резинки к груди") -> "Тяните резинку к груди локтями назад, не поднимая плечи к ушам."
    name.contains("Тяга верхнего блока") -> "Сохраняйте корпус стабильным и тяните рукоять к верхней части груди, сводя лопатки."
    name.contains("Тяга сидя") -> "Сядьте ровно и тяните рукоять к животу, удерживая спину нейтральной."
    name.contains("Птица-собака") || name.contains("Bird-dog") -> "Из положения на четвереньках одновременно вытяните противоположные руку и ногу, не разворачивая таз."
    name.contains("Разведение рук лёжа") -> "Лягте на живот, слегка согните локти и поднимайте руки в стороны плавным движением."
    name.contains("Тяга гантели в наклоне") -> "Наклонитесь с ровной спиной, тяните гантель к тазу и медленно опускайте."
    name.contains("I-Y-T") -> "Лёжа на животе, поочерёдно поднимайте руки в формах I, Y и T, не зажимая шею."
    name.contains("Подтягивания") -> "Тяните грудь к перекладине за счёт спины, сохраняя контроль и не раскачиваясь."
    name.contains("Жим лёгкой гири сидя") -> "Сядьте устойчиво и выжмите гирю вверх, сохраняя запястье и корпус в нейтральном положении."
    name.contains("Жим гантелей сидя") -> "Сядьте ровно, гантели на уровне плеч и плавно выжимайте их вверх."
    name.contains("Жим гантелей над головой") -> "Выжимайте гантели вверх по вертикальной траектории, не разгибая поясницу чрезмерно."
    name.contains("Сгибание рук") -> "Прижмите локти к корпусу, сгибайте руки без раскачивания и медленно возвращайте вес."
    name.contains("Разгибание рук") -> "Зафиксируйте локти и разгибайте руки контролируемо, не помогая корпусом."
    name.contains("Подъём рук в стороны") || name.contains("Подъём гантелей через стороны") -> "Поднимайте руки в стороны примерно до уровня плеч, сохраняя лёгкий сгиб в локтях."
    name.contains("Dead Bug") -> "Лёжа на спине, прижимайте поясницу к полу и поочерёдно выпрямляйте противоположные руку и ногу."
    name.contains("Планка") -> "Опирайтесь на предплечья или ладони, держите тело прямой линией и напрягите пресс."
    name.contains("Скручивания") -> "Поднимайте плечи за счёт мышц пресса, не тяните голову руками и медленно опускайтесь."
    name.contains("Анти-ротационный жим") -> "Держите корпус неподвижно и выжимайте рукоять от груди, сопротивляясь повороту туловища."
    name.contains("Приседания к опоре") -> "Стопы примерно на ширине плеч, отводите таз назад и садитесь до комфортной глубины."
    name.contains("Приседания с весом тела") -> "Стопы на ширине плеч, колени движутся по линии носков, поднимайтесь через всю стопу."
    name.contains("Гоблет-приседания") -> "Держите вес у груди, садитесь вниз с прямым корпусом и поднимайтесь, сохраняя колени по линии носков."
    name.contains("Приседания со штангой") -> "Перед началом закрепите штангу, напрягите корпус и выполняйте присед под контролем; не округляйте спину."
    name.contains("Выпады") -> "Шагните вперёд или назад, опустите таз вертикально вниз и вернитесь в исходное положение."
    name.contains("Болгарские сплит-приседания") -> "Заднюю ногу разместите на опоре, передней выполняйте подконтрольное приседание, сохраняя колено по линии стопы."
    name.contains("Ягодичный мост") -> "Лягте на спину, согните колени и поднимайте таз, сжимая ягодицы в верхней точке."
    name.contains("Подъёмы на носки") -> "Поднимайтесь на носки плавно, задержитесь наверху и медленно опускайтесь, сохраняя равновесие."
    name.contains("Ходьба") -> "Идите в удобном темпе, постепенно разогревая мышцы; сохраняйте свободное дыхание и ровную осанку."
    name.contains("Бег") -> "Начните с комфортного темпа, держите ровный ритм шага и не повышайте интенсивность резко."
    name.contains("Динамическая разминка") -> "Выполняйте лёгкие движения для суставов и мышц в постепенно увеличивающейся амплитуде."
    else -> "Выполняйте упражнение плавно, контролируйте движение и сохраняйте нейтральное положение корпуса."
}

private class LifeViewModel {
    // Populated asynchronously by App.initialize; never read users.json in
    // the constructor because the constructor runs during Compose rendering.
    private var users: MutableMap<String, UserData> = mutableMapOf()
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val saveMutex = Mutex()
    private var saveJob: Job? = null
    var username by mutableStateOf(""); private set
    var data by mutableStateOf(UserData(), referentialEqualityPolicy()); private set
    var activeTab by mutableStateOf("ЗАДАЧИ"); var taskView by mutableStateOf("ACTIVE"); var healthTab by mutableStateOf("ТРЕНИРОВКИ"); var selectedTaskDate by mutableStateOf(todayIso()); var selectedWorkDate by mutableStateOf(todayIso()); var selectedTrainingDate by mutableStateOf(todayIso()); var calendarR by mutableStateOf(false)
    var focusedTask by mutableStateOf<TaskItem?>(null); var dialog by mutableStateOf<DialogState?>(null)
    var editorMode by mutableStateOf<EditorMode?>(null); var editorTask by mutableStateOf<TaskItem?>(null)
    var trainingCreatorOpen by mutableStateOf(false)
    var trainingCreatorGroup by mutableStateOf("")
    var trainingCreatorDate by mutableStateOf("")
    var selectedNoteId by mutableStateOf("")
    var selectedPlanId by mutableStateOf("")
    var nutritionDate by mutableStateOf(todayIso())
    var sleepDate by mutableStateOf(todayIso())
    var settingsOpen by mutableStateOf(false)
    var menuOpen by mutableStateOf(false)
    var avatarEditorOpen by mutableStateOf(false)
    var notificationsOpen by mutableStateOf(false)
    var timeTravelOpen by mutableStateOf(false)
    var timeOffsetMinutes by mutableIntStateOf(0)
        private set
    var trainingUiRevision by mutableIntStateOf(0)
        private set
    private var metricCacheRevision=-1
    private var lastPenaltyCheckDate = ""
    private val completedDaysCache=mutableMapOf<String,Int>()
    private val dayProgressCache=mutableMapOf<String,Float>()
    private fun invalidateTrainingCache(){metricCacheRevision=trainingUiRevision;completedDaysCache.clear();dayProgressCache.clear()}
    private fun ensureTrainingCache(){if(metricCacheRevision!=trainingUiRevision){metricCacheRevision=trainingUiRevision;completedDaysCache.clear();dayProgressCache.clear()}}
    val isAdmin get() = data.role == "Системный администратор"
    val tasks get() = data.tasks

    fun appNow() = kotlin.time.Instant.fromEpochSeconds(
        Clock.System.now().epochSeconds + timeOffsetMinutes.toLong() * 60L
    ).toLocalDateTime(TimeZone.currentSystemDefault())

    fun appTodayIso(): String = appNow().date.toString()

    fun formattedAppNow(): String {
        val d = appNow()
        var h = d.hour
        val ap = if (h < 12) "AM" else "PM"
        h %= 12
        if (h == 0) h = 12
        return "${d.day.toString().padStart(2,'0')}.${(d.month.ordinal + 1).toString().padStart(2,'0')}.${d.year} • $h:${d.minute.toString().padStart(2,'0')} $ap"
    }

    fun timeOffsetText(): String {
        val m=timeOffsetMinutes
        if(m==0) return "Реальное время"
        val sign=if(m>0) "+" else "−"
        val a=kotlin.math.abs(m)
        val h=a/60; val min=a%60
        return "Смещение: $sign${if(h>0) "${h}ч " else ""}${if(min>0) "${min}мин" else ""}".trim()
    }

    fun shiftTestTime(minutes:Int){
        timeOffsetMinutes += minutes
        val today=appTodayIso()
        selectedTaskDate=today; selectedWorkDate=today; selectedTrainingDate=today
        tick()
    }

    fun resetTestTime(){
        timeOffsetMinutes=0
        val today=appTodayIso()
        selectedTaskDate=today; selectedWorkDate=today; selectedTrainingDate=today
        tick()
    }
    private fun UserData.snapshotForSave(): UserData = copy(
        achievements = achievements.map { it.copy() }.toMutableList(),
        tasks = tasks.map { it.copy() }.toMutableList(),
        notifications = notifications.toMutableList(),
        read_notifications = read_notifications.toMutableList(),
        calendar_marks = calendar_marks.toMutableMap(),
        training_completions = training_completions.toMutableList(),
        training_hidden = training_hidden.toMutableList(),
        dishes = dishes.map { it.copy() }.toMutableList(),
        custom_trainings = custom_trainings.map { it.copy() }.toMutableList(),
        training_segment_overrides = training_segment_overrides.toMutableMap(),
        training_plan_by_date = training_plan_by_date.toMutableMap(),
        training_last_activity_by_group = training_last_activity_by_group.toMutableMap(),
        training_penalty_segments = training_penalty_segments.toMutableMap(),
        notes = notes.map { it.copy(linked_note_ids = it.linked_note_ids.toMutableList()) }.toMutableList(),
        detailed_plans = detailed_plans.map { plan -> plan.copy(subitems = plan.subitems.map { it.copy() }.toMutableList(), linked_plan_ids = plan.linked_plan_ids.toMutableList()) }.toMutableList(),
        food_entries = food_entries.map { it.copy() }.toMutableList(),
        sleep_records = sleep_records.map { it.copy() }.toMutableList(),
        finance_accounts = finance_accounts.map { it.copy() }.toMutableList(),
        finance_entries = finance_entries.map { it.copy() }.toMutableList(),
        finance_budgets = finance_budgets.map { it.copy() }.toMutableList()
    )

    private fun enqueueSave(snapshot: Map<String, UserData>) {
        saveJob?.cancel()
        saveJob = saveScope.launch {
            saveMutex.withLock {
                PlatformStorage.saveUsers(snapshot)
            }
        }
    }

    private fun ensureDefaultAccounts() {
        var changed = false
        if (!users.containsKey("admin")) {
            users["admin"] = UserData(password = "12345", role = "Системный администратор", display_name = "admin")
            changed = true
        }
        if (!users.containsKey("Reg1nald")) {
            users["Reg1nald"] = UserData(password = "Qpzmal01", role = "Пользователь", display_name = "Reg1nald")
            changed = true
        }
        if (!users.containsKey("login")) {
            users["login"] = UserData(password = "Reg1nald", role = "Пользователь", display_name = "login")
            changed = true
        }
        if (!users.containsKey("REG1")) {
            users["REG1"] = UserData(password = "123", role = "Пользователь", display_name = "REG1")
            changed = true
        }
        if (changed) enqueueSave(users.mapValues { it.value.snapshotForSave() })
    }
    fun initialize(loadedUsers: MutableMap<String, UserData>, savedUsername: String?): Boolean {
        users = loadedUsers
        ensureDefaultAccounts()
        return restoreSession(savedUsername)
    }

    fun restoreSession(savedUsername: String?): Boolean {
        val saved = savedUsername?.takeIf { it.isNotBlank() } ?: return false
        val key = users.keys.firstOrNull { it.equals(saved, ignoreCase = true) } ?: return false
        val found = users[key] ?: return false
        username = key
        data = found
        normalize()
        applyTrainingInactivityPenalties()
        data = data.copy()
        save()
        trainingUiRevision = 0
        metricCacheRevision = -1
        completedDaysCache.clear(); dayProgressCache.clear()
        return true
    }

    fun login(u:String,p:String):Boolean {
        val login = u.trim()
        val key = users.keys.firstOrNull { it.equals(login, ignoreCase=true) } ?: return false
        val found = users[key] ?: return false
        if (found.password == p) {
            username = key
            data = found
            normalize()
            applyTrainingInactivityPenalties()
            PlatformStorage.saveSessionUsername(key)
            trainingUiRevision=0
            metricCacheRevision=-1
            completedDaysCache.clear();dayProgressCache.clear()
            data = data.copy()
            save()
            return true
        }
        return false
    }
    private fun normalize(){
        // Guard corrupted/legacy progression values. The header calculates the
        // next-level XP target during the first frame, so a negative or enormous
        // saved level must not throw or perform unbounded work on the UI thread.
        data.level = data.level.coerceIn(0, 10_000)
        data.level_xp = data.level_xp.coerceAtLeast(0)
        data.reputation_xp = data.reputation_xp.coerceAtLeast(0)
        data.reputation_rank = data.reputation_rank.coerceAtLeast(0)
        // v80 migration: v79 stored old +/- test values as persistent segment
        // progress. Those legacy values could make a fresh training day appear
        // to occupy two cells (for example one old full cell + a 50% active cell).
        // Real progress is calculated from completed exercises/days, so discard
        // only the obsolete override map once. The +/- controls can be used again
        // with the corrected four-cell model afterward.
        if(data.app_data_version < 21){
            data.training_segment_overrides.clear()
            data.app_data_version = 21
        }
        if(data.training_plan_by_date.isEmpty()) data.training_plan_by_date = mutableMapOf()
        if(data.training_last_activity_by_group.isEmpty()) data.training_last_activity_by_group = mutableMapOf()
        if(data.training_penalty_segments.isEmpty()) data.training_penalty_segments = mutableMapOf()
        // v23 migration: legacy Saturday running entries were stored under legs.
        // Move only those generated completion keys; preserve all other old records.
        if(data.app_data_version < 23) {
            data.training_completions = data.training_completions.map { key ->
                val parts = key.split('|')
                if(parts.size >= 3 && parts[1] == "Ноги" &&
                    runCatching { LocalDate.parse(parts[0]).dayOfWeek.ordinal == 5 }.getOrDefault(false)) {
                    listOf(parts[0], "Выносливость", parts.drop(2).joinToString("|")).joinToString("|")
                } else key
            }.distinct().toMutableList()
            data.app_data_version = 23
        }
        if (data.app_data_version < 24) {
            // v24 adds notes, detailed plans, nutrition, sleep and finance modules.
            // Their fields are defaulted during decoding, so legacy users retain all old records.
            if (data.notes.isEmpty()) data.notes = mutableListOf()
            if (data.detailed_plans.isEmpty()) data.detailed_plans = mutableListOf()
            if (data.food_entries.isEmpty()) data.food_entries = mutableListOf()
            if (data.sleep_records.isEmpty()) data.sleep_records = mutableListOf()
            if (data.finance_accounts.isEmpty()) data.finance_accounts = mutableListOf()
            if (data.finance_entries.isEmpty()) data.finance_entries = mutableListOf()
            if (data.finance_budgets.isEmpty()) data.finance_budgets = mutableListOf()
            data.app_data_version = 24
        }
        data.training_start_date = mondayOf(data.training_start_date.ifBlank { appTodayIso() }).toString()
        data.tasks.forEach { task ->
            if (task.muscle_group == "Руки") task.muscle_group = "Руки и плечи"
            task.points = points(task.difficulty)
            if (task.level_xp < 0) task.level_xp = 0
            task.created_date = task.created_date.ifBlank { task.date }
            if (task.done && task.xp_awarded_amount <= 0) task.xp_awarded_amount = task.points
        }
        data.display_name = data.display_name.ifBlank { username }
    }
    private fun save(){
        if (username.isBlank()) return
        users[username] = data
        saveJob?.cancel()
        saveJob = saveScope.launch {
            delay(200.milliseconds)
            val key = username
            val current = data.snapshotForSave()
            val snapshot: Map<String, UserData> = users.toMap() + (key to current)
            saveMutex.withLock { PlatformStorage.saveUsers(snapshot) }
        }
    }
    private fun recordId(prefix: String, size: Int): String = "$prefix-${Clock.System.now().epochSeconds}-$size"

    fun createNote() {
        val note = NoteItem(id = recordId("note", data.notes.size), title = "Новая заметка", topic = "Без темы")
        data = data.copy(notes = (data.notes + note).toMutableList())
        selectedNoteId = note.id
        activeTab = "ЗАМЕТКИ"
        save()
    }
    fun updateNote(updated: NoteItem) {
        val oldLinks = data.notes.firstOrNull { it.id == updated.id }?.linked_note_ids.orEmpty()
        val validLinks = updated.linked_note_ids.filter { id -> id != updated.id && data.notes.any { it.id == id } }.distinct()
        data = data.copy(notes = data.notes.map { current ->
            when {
                current.id == updated.id -> updated.copy(linked_note_ids = validLinks.toMutableList(), updated_at = todayIso())
                validLinks.contains(current.id) && !current.linked_note_ids.contains(updated.id) -> current.copy(linked_note_ids = (current.linked_note_ids + updated.id).distinct().toMutableList())
                oldLinks.contains(current.id) && !validLinks.contains(current.id) -> current.copy(linked_note_ids = current.linked_note_ids.filterNot { it == updated.id }.toMutableList())
                else -> current
            }
        }.toMutableList())
        save()
    }
    fun removeNote(id: String) {
        data = data.copy(notes = data.notes.filterNot { it.id == id }.map { it.copy(linked_note_ids = it.linked_note_ids.filterNot { linked -> linked == id }.toMutableList()) }.toMutableList())
        selectedNoteId = data.notes.firstOrNull()?.id.orEmpty()
        save()
    }
    fun createDetailedPlan(title: String = "Новый план", central: Boolean = false) {
        val plan = DetailedPlanItem(id = recordId("plan", data.detailed_plans.size), title = title.ifBlank { "Новый план" }, is_central = central)
        val others = if (central) data.detailed_plans.map { it.copy(is_central = false) } else data.detailed_plans
        data = data.copy(detailed_plans = (others + plan).toMutableList())
        selectedPlanId = plan.id
        activeTab = "ПЛАНЫ В ПОДРОБНОСТЯХ"
        save()
    }
    fun updateDetailedPlan(updated: DetailedPlanItem) {
        val oldLinks = data.detailed_plans.firstOrNull { it.id == updated.id }?.linked_plan_ids.orEmpty()
        val validLinks = updated.linked_plan_ids.filter { id -> id != updated.id && data.detailed_plans.any { it.id == id } }.distinct()
        data = data.copy(detailed_plans = data.detailed_plans.map { current ->
            when {
                current.id == updated.id -> updated.copy(
                    subitems = updated.subitems.map { it.copy() }.toMutableList(),
                    linked_plan_ids = validLinks.toMutableList(),
                    updated_at = todayIso()
                )
                validLinks.contains(current.id) && !current.linked_plan_ids.contains(updated.id) -> current.copy(linked_plan_ids = (current.linked_plan_ids + updated.id).distinct().toMutableList())
                oldLinks.contains(current.id) && !validLinks.contains(current.id) -> current.copy(linked_plan_ids = current.linked_plan_ids.filterNot { it == updated.id }.toMutableList())
                else -> current
            }
        }.toMutableList())
        save()
    }
    fun setCentralPlan(id: String, central: Boolean) {
        data = data.copy(detailed_plans = data.detailed_plans.map {
            when {
                it.id == id -> it.copy(is_central = central, map_level = 0, map_angle = -1.0)
                central -> it.copy(is_central = false)
                else -> it
            }
        }.toMutableList())
        save()
    }
    fun setPlanMapPlacement(id: String, level: Int, angle: Double) {
        data = data.copy(detailed_plans = data.detailed_plans.map {
            if (it.id == id && !it.is_central) it.copy(map_level = level, map_angle = angle) else it
        }.toMutableList())
        save()
    }
    fun resetPlanMapPlacement() {
        data = data.copy(detailed_plans = data.detailed_plans.map { it.copy(map_level = 0, map_angle = -1.0) }.toMutableList())
        save()
    }
    fun addPlanSubItem(planId: String) {
        val plan = data.detailed_plans.firstOrNull { it.id == planId } ?: return
        val item = PlanSubItem(id = recordId("part", plan.subitems.size), text = "Новый подпункт")
        updateDetailedPlan(plan.copy(subitems = (plan.subitems + item).toMutableList()))
    }
    fun updatePlanSubItem(planId: String, updated: PlanSubItem) {
        val plan = data.detailed_plans.firstOrNull { it.id == planId } ?: return
        updateDetailedPlan(plan.copy(subitems = plan.subitems.map { if (it.id == updated.id) updated else it }.toMutableList()))
    }
    fun removeDetailedPlan(id: String) {
        data = data.copy(detailed_plans = data.detailed_plans.filterNot { it.id == id }.map { it.copy(linked_plan_ids = it.linked_plan_ids.filterNot { linked -> linked == id }.toMutableList()) }.toMutableList())
        selectedPlanId = data.detailed_plans.firstOrNull()?.id.orEmpty()
        save()
    }
    fun addFoodEntry(name: String, meal: String, calories: Int, protein: Double, fat: Double, carbs: Double) {
        val entry = FoodEntry(id = recordId("food", data.food_entries.size), date = nutritionDate, meal = meal, name = name.ifBlank { "Продукт" }, calories = calories.coerceAtLeast(0), protein_grams = protein.coerceAtLeast(0.0), fat_grams = fat.coerceAtLeast(0.0), carbs_grams = carbs.coerceAtLeast(0.0))
        data = data.copy(food_entries = (data.food_entries + entry).toMutableList())
        save()
    }
    private fun defaultDishes(): List<Dish> = listOf(
        Dish("d1", "Гречка с мясом", "Завтрак", 450, 25.0, 30.0, 40.0, "1. Отварите 100 г гречки до готовности.\n2. Обжарьте 120 г нежирной говядины или курицы с луком 10–12 минут.\n3. Смешайте с гречкой, посолите, добавьте зелень."),
        Dish("d2", "Овсянка с бананом", "Завтрак", 320, 10.0, 6.0, 56.0, "1. Залейте 60 г хлопьев 200 мл молока или воды.\n2. Варите 5 минут, помешивая.\n3. Добавьте нарезанный банан и корицу."),
        Dish("d3", "Омлет с овощами", "Завтрак", 280, 18.0, 20.0, 6.0, "1. Взбейте 3 яйца с щепоткой соли.\n2. Обжарьте нарезанные помидор и перец 3 минуты.\n3. Залейте яйцами и готовьте под крышкой 5 минут."),
        Dish("d4", "Творог с ягодами", "Завтрак", 240, 24.0, 5.0, 22.0, "1. Выложите 200 г творога 2–5% в миску.\n2. Добавьте горсть ягод и ложку мёда.\n3. Перемешайте и подавайте сразу."),
        Dish("d5", "Куриная грудка с рисом", "Обед", 520, 42.0, 9.0, 68.0, "1. Отварите 80 г риса.\n2. Грудку 150 г нарежьте, посолите и запеките или обжарьте 12–15 минут.\n3. Подавайте с овощным салатом."),
        Dish("d6", "Борщ", "Обед", 350, 18.0, 16.0, 32.0, "1. Сварите бульон из говядины.\n2. Добавьте картофель, затем свёклу, морковь, капусту и томатную пасту.\n3. Варите до мягкости, подавайте со сметаной."),
        Dish("d7", "Паста с индейкой", "Обед", 580, 38.0, 14.0, 75.0, "1. Отварите 90 г пасты.\n2. Обжарьте 150 г индейки с чесноком и томатами.\n3. Смешайте с пастой, посыпьте сыром."),
        Dish("d8", "Рыба с овощами на пару", "Ужин", 380, 34.0, 16.0, 18.0, "1. Выложите филе рыбы и овощи в пароварку.\n2. Посолите, добавьте лимонный сок.\n3. Готовьте 15–20 минут."),
        Dish("d9", "Тушёная курица с овощами", "Ужин", 410, 38.0, 18.0, 22.0, "1. Обжарьте кусочки курицы 5 минут.\n2. Добавьте кабачок, перец, морковь и немного воды.\n3. Тушите под крышкой 20 минут."),
        Dish("d10", "Запечённый лосось с картофелем", "Ужин", 520, 36.0, 24.0, 40.0, "1. Нарежьте картофель дольками, смажьте маслом.\n2. Выложите рядом лосось, приправьте.\n3. Запекайте 25 минут при 190 °C."),
        Dish("d11", "Яблоко с арахисовой пастой", "Перекус", 210, 5.0, 12.0, 24.0, "1. Нарежьте яблоко дольками.\n2. Подавайте с ложкой арахисовой пасты без сахара."),
        Dish("d12", "Йогурт с орехами", "Перекус", 230, 12.0, 12.0, 18.0, "1. Выложите 150 г натурального йогурта.\n2. Посыпьте горстью орехов (20 г)."),
        Dish("d13", "Смузи банан-кефир", "Перекус", 180, 7.0, 3.0, 32.0, "1. Положите в блендер банан и 200 мл кефира.\n2. Взбейте до однородности.")
    )
    fun ensureDishes() {
        if (data.nutrition_calorie_target == 1568) data = data.copy(nutrition_calorie_target = 2500)
        if (!data.dishes_seeded) {
            data = data.copy(dishes = (defaultDishes() + data.dishes).toMutableList(), dishes_seeded = true)
            save()
        }
    }
    fun saveDish(dish: Dish) {
        val clean = dish.copy(name = dish.name.ifBlank { "Блюдо" }.take(60), calories = dish.calories.coerceAtLeast(0))
        data = if (clean.id.isBlank()) data.copy(dishes = (data.dishes + clean.copy(id = recordId("dish", data.dishes.size))).toMutableList())
        else data.copy(dishes = data.dishes.map { if (it.id == clean.id) clean else it }.toMutableList())
        save()
    }
    fun deleteDish(id: String) {
        data = data.copy(dishes = data.dishes.filterNot { it.id == id }.toMutableList())
        save()
    }
    fun addDishToDiary(dish: Dish) = addFoodEntry(dish.name, dish.meal, dish.calories, dish.protein_grams, dish.fat_grams, dish.carbs_grams)
    fun removeDishFromDiary(dish: Dish) {
        val e = data.food_entries.lastOrNull { it.date == nutritionDate && it.name == dish.name } ?: return
        deleteFoodEntry(e.id)
    }
    fun deleteFoodEntry(id: String) {
        data = data.copy(food_entries = data.food_entries.filterNot { it.id == id }.toMutableList())
        save()
    }
    fun updateNutritionTarget(target: Int) {
        data = data.copy(nutrition_calorie_target = target.coerceAtLeast(1))
        save()
    }
    fun saveSleepRecord(record: SleepRecord) {
        data = data.copy(sleep_records = (data.sleep_records.filterNot { it.date == record.date } + record).sortedBy { it.date }.toMutableList())
        save()
    }
    fun ensureFinanceAccount() {
        if (data.finance_accounts.isEmpty()) {
            data = data.copy(finance_accounts = mutableListOf(FinanceAccount(id = recordId("account", 0), name = "Основной счёт", opening_balance = 0.0, bank_name = "Счёт")))
            save()
        }
    }
    fun addFinanceAccount(name: String, initialBalance: Double) {
        val account = FinanceAccount(id = recordId("account", data.finance_accounts.size), name = name.ifBlank { "Новый счёт" }, opening_balance = initialBalance, bank_name = "Счёт")
        data = data.copy(finance_accounts = (data.finance_accounts + account).toMutableList())
        save()
    }
    fun updateFinanceAccount(updated: FinanceAccount) {
        data = data.copy(finance_accounts = data.finance_accounts.map { if (it.id == updated.id) updated else it }.toMutableList())
        save()
    }
    fun addFinanceEntry(type: String, title: String, category: String, amount: Double, accountId: String) {
        if (data.finance_accounts.isEmpty()) ensureFinanceAccount()
        val id = accountId.ifBlank { data.finance_accounts.firstOrNull()?.id.orEmpty() }
        val entry = FinanceEntry(id = recordId("money", data.finance_entries.size), type = type, title = title.ifBlank { if (type == "Доход") "Доход" else "Расход" }, category = category.ifBlank { "Прочее" }, amount = amount.coerceAtLeast(0.0), account_id = id)
        data = data.copy(finance_entries = (data.finance_entries + entry).toMutableList())
        save()
    }
    fun deleteFinanceEntry(id: String) {
        data = data.copy(finance_entries = data.finance_entries.filterNot { it.id == id }.toMutableList())
        save()
    }
    fun upsertFinanceBudget(category: String, limit: Double) {
        val clean = category.ifBlank { "Прочее" }
        val existing = data.finance_budgets.firstOrNull { it.category.equals(clean, ignoreCase = true) }
        val item = FinanceBudget(id = existing?.id ?: recordId("budget", data.finance_budgets.size), category = clean, limit_amount = limit.coerceAtLeast(0.0))
        data = data.copy(finance_budgets = (data.finance_budgets.filterNot { it.category.equals(clean, ignoreCase = true) } + item).toMutableList())
        save()
    }

    fun unread()=data.notifications.count{!data.read_notifications.contains(it)}
    fun completedCount(difficulty:String)=data.tasks.count{it.done&&it.difficulty==difficulty}
    fun createdCount(date:String)=data.tasks.count{it.created_date==date}
    fun completedCountOn(date:String)=data.tasks.count{it.completed_date==date}

    private fun defaultTrainingGroupFor(date:String):String = when(LocalDate.parse(date).dayOfWeek.ordinal){
        0 -> "Грудь"
        1 -> "Спина"
        2 -> "Руки и плечи"
        3 -> "Пресс"
        4 -> "Ноги"
        5 -> "Выносливость"
        else -> ""
    }

    fun trainingGroupFor(date:String):String =
        if (data.training_plan_by_date.containsKey(date)) data.training_plan_by_date[date].orEmpty()
        else defaultTrainingGroupFor(date)

    fun trainingPlanLabelFor(date:String):String =
        if (data.training_plan_by_date.containsKey(date)) data.training_plan_by_date[date].orEmpty().ifBlank { "Отдых" }
        else if (LocalDate.parse(date).dayOfWeek.ordinal == 5) "Выносливость (бег)"
        else defaultTrainingGroupFor(date).ifBlank { "Отдых" }

    fun setTrainingGroupForDate(date:String, group:String) {
        val map = data.training_plan_by_date.toMutableMap()
        map[date] = group
        data = data.copy(training_plan_by_date = map)
        trainingUiRevision++
        invalidateTrainingCache()
        save()
    }

    private fun trainingKey(date:String,group:String,index:Int):String = "$date|$group|$index"
    private fun customTrainingKey(id:String):String = "CUSTOM|$id"
    private fun dateOrdinal(date:LocalDate):Int {
        var days = 365 * date.year + (date.year / 4) - (date.year / 100) + (date.year / 400)
        val monthDays=intArrayOf(31,28,31,30,31,30,31,31,30,31,30,31)
        var m=1
        while(m < date.month.ordinal + 1){
            days += monthDays[m-1]
            if(m==2 && isLeapYear(date.year)) days += 1
            m++
        }
        return days + date.day - 1
    }

    private fun mondayOf(dateText:String):LocalDate {
        // Old/hand-edited user files occasionally contain an invalid date.
        // Use today as a safe migration fallback instead of aborting startup.
        val d=runCatching { LocalDate.parse(dateText) }.getOrElse { LocalDate.parse(appTodayIso()) }
        var year=d.year
        var month=d.month.ordinal+1
        var day=d.day
        var back=d.dayOfWeek.ordinal
        while(back>0){
            if(day>1) day-- else {
                month--
                if(month<1){month=12;year--}
                day=daysInMonth(year,month)
            }
            back--
        }
        return LocalDate(year,month,day)
    }

    fun canExecuteTraining(date:String):Boolean = isAdmin || date == appTodayIso()

    fun customTrainingExercises(group:String,date:String):List<CustomTrainingExercise> =
        data.custom_trainings.filter{it.muscle_group==group && it.date==date}.sortedBy{it.created_at}

    fun isTrainingExerciseDone(date:String,group:String,index:Int):Boolean =
        data.training_completions.contains(trainingKey(date,group,index))

    fun isTrainingExerciseDone(ex:TrainingExercise):Boolean = if(ex.isCustom){
        val id=ex.key.removePrefix("CUSTOM|")
        data.custom_trainings.firstOrNull{it.id==id}?.done==true
    } else {
        data.training_completions.contains(ex.key)
    }

    fun toggleTrainingExercise(ex:TrainingExercise){
        var activityDate = ""
        var activityGroup = ""
        if(ex.isCustom){
            val id=ex.key.removePrefix("CUSTOM|")
            val c=data.custom_trainings.firstOrNull{it.id==id} ?: return
            if(!canExecuteTraining(c.date)) return
            c.done=!c.done
            if(c.done){ activityDate=c.date; activityGroup=c.muscle_group }
        } else {
            val parts = ex.key.split('|')
            val date = parts.getOrNull(0).orEmpty()
            val group = parts.getOrNull(1).orEmpty()
            if(!canExecuteTraining(date)) return
            val updated=data.training_completions.toMutableList()
            if(updated.contains(ex.key)) updated.remove(ex.key) else {
                updated.add(ex.key)
                activityDate=date; activityGroup=group
            }
            data = data.copy(training_completions=updated)
        }
        if(activityDate.isNotBlank() && activityGroup.isNotBlank()){
            val last = data.training_last_activity_by_group.toMutableMap()
            last[activityGroup] = activityDate
            data = data.copy(training_last_activity_by_group=last)
        }
        data=data.copy()
        trainingUiRevision++
        invalidateTrainingCache()
        applyTrainingInactivityPenalties()
        save()
    }

    /**
     * Day progress is calculated from the actual number of exercises scheduled
     * for this date. Every exercise has equal weight; there is no fixed 4-exercise
     * assumption. 1/4=25%, 1/5=20%, 1/6=16.67%, 5/5=100%, etc.
     */
    fun trainingDayProgress(group:String,date:String):Float {
        ensureTrainingCache()
        val cacheKey="$group|$date"
        dayProgressCache[cacheKey]?.let{return it}
        if(trainingGroupFor(date)!=group){dayProgressCache[cacheKey]=0f;return 0f}

        val total=trainingExerciseCount(group,date)
        if(total<=0){dayProgressCache[cacheKey]=0f;return 0f}

        val custom=customTrainingExercises(group,date)
        val doneGenerated=visibleGeneratedIndices(group,date).count{isTrainingExerciseDone(date,group,it)}
        val doneCustom=custom.count{it.done}
        val done=(doneGenerated+doneCustom).coerceIn(0,total)
        val result=done.toFloat()/total.toFloat()
        dayProgressCache[cacheKey]=result
        return result
    }

    /** Whole-number UI percentage; bar rendering keeps the precise Float above. */
    fun trainingDayPercent(group:String,date:String):Int =
        kotlin.math.round(trainingDayProgress(group,date)*100f).toInt().coerceIn(0,100)

    /** Total exercises for this day, including custom exercises. */
    private fun trainingExerciseCount(group:String,date:String):Int {
        if(group.isBlank()) return 0
        return visibleGeneratedIndices(group,date).size+customTrainingExercises(group,date).size
    }

    fun completedTrainingDays(group:String):Int {
        ensureTrainingCache()
        completedDaysCache[group]?.let{return it}
        val dates=(data.training_completions.mapNotNull{it.substringBefore('|').takeIf{it.isNotBlank()}} + data.custom_trainings.filter{it.muscle_group==group}.map{it.date}).distinct()
        var count=0
        dates.forEach{d->
            if(trainingGroupFor(d)==group){
                val vis=visibleGeneratedIndices(group,d)
                val custom=customTrainingExercises(group,d)
                val generatedAll=vis.all{isTrainingExerciseDone(d,group,it)}
                val customAll=custom.all{it.done}
                if(generatedAll && customAll && (vis.size+custom.size)>0) count++
            }
        }
        completedDaysCache[group]=count
        return count
    }

    private fun trainingPenaltyGroups():List<String> = listOf("Грудь","Спина","Руки и плечи","Пресс","Ноги","Выносливость")

    private fun historicalTrainingUnits(group:String):Float {
        val dates = (data.training_completions.mapNotNull { key ->
            val p = key.split('|')
            if (p.size >= 3 && p[1] == group) p[0] else null
        } + data.custom_trainings.filter { it.muscle_group == group && it.done }.map { it.date }).distinct()
        var units = 0f
        for (date in dates) {
            val vis = visibleGeneratedIndices(group, date)
            val custom = customTrainingExercises(group, date)
            val generatedDone = vis.count { isTrainingExerciseDone(date, group, it) }
            val customDone = custom.count { it.done }
            val total = vis.size + custom.size
            if (total > 0) units += (generatedDone + customDone).coerceIn(0, total).toFloat() / total.toFloat()
        }
        return units
    }

    private fun applyTrainingInactivityPenalties() {
        val lastMap = data.training_last_activity_by_group.toMutableMap()
        val penaltyMap = data.training_penalty_segments.toMutableMap()
        val completionDates = data.training_completions.mapNotNull { key ->
            val parts = key.split('|')
            if (parts.size >= 3 && runCatching { LocalDate.parse(parts[0]) }.isSuccess) parts[1] to parts[0] else null
        }
        trainingPenaltyGroups().forEach { group ->
            val latest = (
                completionDates.filter { it.first == group }.map { it.second } +
                    data.custom_trainings.filter {
                        it.muscle_group == group && it.done && runCatching { LocalDate.parse(it.date) }.isSuccess
                    }.map { it.date }
                ).maxByOrNull { dateText -> dateOrdinal(LocalDate.parse(dateText)) }

            val storedLast = lastMap[group]
            val validStoredLast = storedLast?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess }
            val last = validStoredLast ?: latest
            if (last != null) {
                lastMap[group] = last
                val elapsed = (dateOrdinal(LocalDate.parse(appTodayIso())) - dateOrdinal(LocalDate.parse(last))).coerceAtLeast(0)
                if (elapsed >= 14) {
                    val expectedPenalty = 1 + ((elapsed - 14) / 7)
                    penaltyMap[group] = maxOf(penaltyMap[group] ?: 0, expectedPenalty)
                }
            } else {
                lastMap.remove(group)
            }
        }
        if (lastMap != data.training_last_activity_by_group || penaltyMap != data.training_penalty_segments) {
            data = data.copy(training_last_activity_by_group = lastMap, training_penalty_segments = penaltyMap)
            save()
        }
    }

    private fun effectiveTrainingUnits(group:String):Float {
        val real = historicalTrainingUnits(group)
        val manual = (data.training_segment_overrides[group] ?: 0).toFloat()
        val penalties = (data.training_penalty_segments[group] ?: 0).toFloat()
        return (maxOf(real, manual) - penalties).coerceAtLeast(0f)
    }

    private fun cellsForLevel(level:Int):Int = 4 + ((level - 1).coerceAtLeast(0) / 5)

    fun muscleLevel(group:String):Int {
        var units = effectiveTrainingUnits(group)
        var level = 1
        while (level < 150 && units >= cellsForLevel(level)) {
            units -= cellsForLevel(level)
            level++
        }
        return level
    }

    fun trainingProgressWeek(group:String):Int = muscleLevel(group)

    fun trainingDifficultyForLevel(level:Int):String = when (level) {
        in 1..4 -> "Очень легко"
        in 5..9 -> "Легко"
        in 10..24 -> "Средне"
        in 25..39 -> "Сложно"
        in 40..74 -> "Очень сложно"
        else -> "Экстремально"
    }

    fun trainingDifficultyForGroup(group:String,date:String=selectedTrainingDate):String {
        val level=muscleLevel(group)
        val dayProgress=if(trainingGroupFor(date)==group) trainingDayProgress(group,date) else 0f
        val projectedLevel=if(level<150 && dayProgress>=0.75f) level+1 else level
        return trainingDifficultyForLevel(projectedLevel)
    }

    /** Segment count increases by one every five levels; the active cell may be fractional. */
    fun muscleSegmentCount(group:String):Int = cellsForLevel(muscleLevel(group))

    fun adjustTrainingSegments(group:String,delta:Int){
        if(!isAdmin) return
        // Admin +/- is a progression test control, not a cosmetic-only override.
        // Store the TOTAL number of completed training segments so crossing
        // Manual admin adjustment adds/subtracts one cumulative progress unit.
        val maxSegments=150*30
        val current=data.training_segment_overrides[group] ?: completedTrainingDays(group)
        val next=(current+delta).coerceIn(0,maxSegments)
        val map=data.training_segment_overrides.toMutableMap()
        map[group]=next
        data=data.copy(training_segment_overrides=map)
        trainingUiRevision++
        invalidateTrainingCache()
        save()
    }

    fun trainingCumulativePercent(group:String):Int =
        kotlin.math.round(effectiveTrainingUnits(group) * 100f).toInt().coerceAtLeast(0)

    fun muscleSegmentProgress(group:String,index:Int):Float {
        val level = muscleLevel(group)
        val count = cellsForLevel(level)
        var remaining = effectiveTrainingUnits(group)
        for (l in 1 until level) remaining -= cellsForLevel(l)
        remaining = remaining.coerceIn(0f, count.toFloat())
        return (remaining - index.toFloat()).coerceIn(0f, 1f)
    }

    fun overallTrainingProgress(group:String):Float {
        val units = effectiveTrainingUnits(group)
        // Normalize progress against a broad 30-level horizon, keeping each muscle
        // on its own radar axis so one group's value cannot move another axis.
        return (kotlin.math.sqrt((units / 120f).coerceIn(0f, 1f))).coerceIn(0f, 1f)
    }

    private fun tierForLevel(level:Int):Int = when (level) {
        in Int.MIN_VALUE..4 -> 0
        in 5..9 -> 1
        in 10..14 -> 2
        in 15..19 -> 3
        in 20..24 -> 4
        else -> 5
    }

    // Each completed calendar week increases the next session slightly. The four-week cycle
    // alternates extra reps, an extra set, then reps+set plus a small load note. Running uses metres.
    private fun weeklyProgressedPrescription(name:String,prescription:String,week:Int):String {
        val safeWeek=week.coerceAtLeast(1)
        if(name.contains("бег",ignoreCase=true)) {
            val numbers=Regex("\\d+").findAll(prescription).map{it.value.toInt()}.toList()
            val base=if(numbers.isEmpty())1000 else numbers.first()*100
            val distance=(base+(safeWeek-1)*200).coerceAtMost(10000)
            return "$distance м"
        }
        if(name.contains("ходьба",ignoreCase=true)) {
            val distance=(400+(safeWeek-1)*100).coerceAtMost(4000)
            return "$distance м"
        }
        val pattern=Regex("^(\\d+)×(\\d+)[–-](\\d+)(.*)$").find(prescription) ?: return prescription
        val baseSets=pattern.groupValues[1].toInt(); val low=pattern.groupValues[2].toInt(); val high=pattern.groupValues[3].toInt(); val suffix=pattern.groupValues[4]
        val cycle=(safeWeek-1)%4
        var sets=baseSets; var l=low; var h=high
        when(cycle){1->{l+=1;h+=1};2->{sets+=1};3->{sets+=1;l+=2;h+=2}}
        val block=(safeWeek-1)/4
        val weighted=name.contains("гант",ignoreCase=true)||name.contains("гир",ignoreCase=true)||name.contains("штанг",ignoreCase=true)||name.contains("блок",ignoreCase=true)||name.contains("резин",ignoreCase=true)
        val loadNote=if(cycle==3 && weighted) " • нагрузка +${(block+1)*5}%" else ""
        return "$sets×$l–$h$suffix$loadNote"
    }

    private fun builtinTrainingBase(group:String,tier:Int,difficulty:String,@Suppress("UNUSED_PARAMETER") weekday:Int):List<TrainingExercise> =
        when (group) {
            "Грудь" -> when(tier){
                0 -> listOf(
                    TrainingExercise("Отжимания от стены","2×8–10",difficulty),
                    TrainingExercise("Жим лёгкой гири лёжа","2×8–10",difficulty),
                    TrainingExercise("Сведение рук без веса","2×10",difficulty),
                    TrainingExercise("Медленные отжимания от опоры","1×6–8",difficulty)
                )
                1 -> listOf(
                    TrainingExercise("Отжимания от опоры","2×10–12",difficulty),
                    TrainingExercise("Жим лёгких гантелей лёжа","2×10–12",difficulty),
                    TrainingExercise("Сведение рук с лёгким весом","2×10–12",difficulty),
                    TrainingExercise("Отжимания с колен","2×8–12",difficulty)
                )
                2 -> listOf(
                    TrainingExercise("Жим гантелей лёжа","3×8–12",difficulty),
                    TrainingExercise("Жим гантелей на наклонной скамье","3×8–10",difficulty),
                    TrainingExercise("Сведения рук","2×10–12",difficulty),
                    TrainingExercise("Отжимания","2×10–15",difficulty)
                )
                3 -> listOf(
                    TrainingExercise("Жим штанги лёжа","3×8–10",difficulty),
                    TrainingExercise("Жим гантелей на наклонной скамье","3×8–10",difficulty),
                    TrainingExercise("Сведения на блоках","3×10–12",difficulty),
                    TrainingExercise("Отжимания с паузой","3×8–12",difficulty)
                )
                4 -> listOf(
                    TrainingExercise("Жим штанги лёжа","4×6–10",difficulty),
                    TrainingExercise("Жим гантелей на наклонной скамье","4×8–10",difficulty),
                    TrainingExercise("Сведения на блоках","3×10–12",difficulty),
                    TrainingExercise("Отжимания с усложнением","3×8–10",difficulty)
                )
                else -> listOf(
                    TrainingExercise("Жим штанги лёжа с контролем","4×5–8",difficulty),
                    TrainingExercise("Жим гантелей на наклонной скамье","4×6–10",difficulty),
                    TrainingExercise("Сведения на блоках с паузой","4×8–12",difficulty),
                    TrainingExercise("Отжимания с дополнительным сопротивлением","4×6–10",difficulty)
                )
            }
            "Спина" -> when(tier){
                0 -> listOf(
                    TrainingExercise("Тяга резинки сидя","2×8–10",difficulty),
                    TrainingExercise("Тяга лёгкой резинки к груди","2×8–10",difficulty),
                    TrainingExercise("Птица-собака","2×6–8/сторону",difficulty),
                    TrainingExercise("Разведение рук лёжа","2×8–10",difficulty)
                )
                1 -> listOf(
                    TrainingExercise("Тяга верхнего блока лёгким весом","2×10–12",difficulty),
                    TrainingExercise("Тяга сидя лёгким весом","2×10–12",difficulty),
                    TrainingExercise("Птица-собака","2×8–10/сторону",difficulty),
                    TrainingExercise("Лёгкая тяга гантели","2×10/сторону",difficulty)
                )
                2 -> listOf(
                    TrainingExercise("Тяга верхнего блока","3×8–12",difficulty),
                    TrainingExercise("Тяга сидя","3×8–12",difficulty),
                    TrainingExercise("Тяга гантели в наклоне","2×8–10/сторону",difficulty),
                    TrainingExercise("I-Y-T подъёмы","2×8–10",difficulty)
                )
                3 -> listOf(
                    TrainingExercise("Тяга верхнего блока","3×8–10",difficulty),
                    TrainingExercise("Тяга сидя","3×8–10",difficulty),
                    TrainingExercise("Тяга штанги в наклоне","3×8–10",difficulty),
                    TrainingExercise("Подтягивания с резинкой","3×6–10",difficulty)
                )
                4 -> listOf(
                    TrainingExercise("Тяга верхнего блока тяжёлым весом","4×6–10",difficulty),
                    TrainingExercise("Тяга сидя","4×8–10",difficulty),
                    TrainingExercise("Тяга штанги в наклоне","4×6–10",difficulty),
                    TrainingExercise("Подтягивания","4×5–8",difficulty)
                )
                else -> listOf(
                    TrainingExercise("Тяга верхнего блока с паузой","4×5–8",difficulty),
                    TrainingExercise("Тяга сидя с контролем","4×6–10",difficulty),
                    TrainingExercise("Тяга штанги в наклоне","4×6–8",difficulty),
                    TrainingExercise("Подтягивания с дополнительным сопротивлением","4×5–8",difficulty)
                )
            }
            "Руки и плечи" -> when(tier){
                0 -> listOf(
                    TrainingExercise("Жим лёгкой гири сидя","2×6–8",difficulty),
                    TrainingExercise("Сгибание рук без веса/с лёгким весом","2×8–10",difficulty),
                    TrainingExercise("Разгибание рук без веса","2×8–10",difficulty),
                    TrainingExercise("Подъём рук в стороны без веса","2×8–10",difficulty)
                )
                1 -> listOf(
                    TrainingExercise("Жим гантелей сидя","2×8–10",difficulty),
                    TrainingExercise("Сгибание рук с гантелями","2×10–12",difficulty),
                    TrainingExercise("Разгибание рук с резинкой","2×10–12",difficulty),
                    TrainingExercise("Подъём гантелей через стороны","2×10",difficulty)
                )
                2 -> listOf(
                    TrainingExercise("Жим гантелей над головой","3×8–12",difficulty),
                    TrainingExercise("Сгибание рук с гантелями","3×10–12",difficulty),
                    TrainingExercise("Разгибание рук назад","3×10–12",difficulty),
                    TrainingExercise("Подъём гантелей через стороны","2×10–12",difficulty)
                )
                3 -> listOf(
                    TrainingExercise("Жим гантелей над головой","3×8–10",difficulty),
                    TrainingExercise("Сгибание рук со штангой","3×8–10",difficulty),
                    TrainingExercise("Разгибание рук на блоке","3×10–12",difficulty),
                    TrainingExercise("Подъём гантелей через стороны","3×10–12",difficulty)
                )
                4 -> listOf(
                    TrainingExercise("Жим гантелей над головой","4×6–10",difficulty),
                    TrainingExercise("Сгибание рук со штангой","4×8–10",difficulty),
                    TrainingExercise("Разгибание рук на блоке","4×8–12",difficulty),
                    TrainingExercise("Подъём гантелей через стороны","3×10–12",difficulty)
                )
                else -> listOf(
                    TrainingExercise("Жим гантелей над головой с контролем","4×5–8",difficulty),
                    TrainingExercise("Сгибание рук со штангой","4×6–10",difficulty),
                    TrainingExercise("Разгибание рук на блоке","4×8–10",difficulty),
                    TrainingExercise("Подъём гантелей через стороны","4×8–12",difficulty)
                )
            }
            "Пресс" -> when(tier){
                0 -> listOf(
                    TrainingExercise("Dead Bug","2×6–8/сторону",difficulty),
                    TrainingExercise("Короткая планка","2×15–20 сек",difficulty),
                    TrainingExercise("Лёгкие скручивания","2×8–10",difficulty),
                    TrainingExercise("Bird-dog","2×6–8/сторону",difficulty)
                )
                1 -> listOf(
                    TrainingExercise("Dead Bug","2×8–10/сторону",difficulty),
                    TrainingExercise("Планка","2×20–30 сек",difficulty),
                    TrainingExercise("Скручивания","2×10–12",difficulty),
                    TrainingExercise("Bird-dog","2×8–10/сторону",difficulty)
                )
                2 -> listOf(
                    TrainingExercise("Скручивания","3×10–15",difficulty),
                    TrainingExercise("Планка","3×30–45 сек",difficulty),
                    TrainingExercise("Dead Bug","3×8–10/сторону",difficulty),
                    TrainingExercise("Анти-ротационный жим","2×10/сторону",difficulty)
                )
                3 -> listOf(
                    TrainingExercise("Скручивания с паузой","3×12–15",difficulty),
                    TrainingExercise("Планка","3×45–60 сек",difficulty),
                    TrainingExercise("Dead Bug","3×10/сторону",difficulty),
                    TrainingExercise("Анти-ротационный жим","3×10/сторону",difficulty)
                )
                4 -> listOf(
                    TrainingExercise("Скручивания с подъёмом ног","4×10–15",difficulty),
                    TrainingExercise("Планка","4×45–60 сек",difficulty),
                    TrainingExercise("Dead Bug с паузой","4×10/сторону",difficulty),
                    TrainingExercise("Анти-ротационный жим","4×10/сторону",difficulty)
                )
                else -> listOf(
                    TrainingExercise("Скручивания с усложнением","4×8–12",difficulty),
                    TrainingExercise("Планка с усложнением","4×45–75 сек",difficulty),
                    TrainingExercise("Dead Bug с контролем","4×10–12/сторону",difficulty),
                    TrainingExercise("Анти-ротационный жим","4×10–12/сторону",difficulty)
                )
            }
            "Ноги" -> when(tier){
                0 -> listOf(
                    TrainingExercise("Приседания к опоре","2×8–10",difficulty),
                    TrainingExercise("Выпады назад с опорой","2×6–8/нога",difficulty),
                    TrainingExercise("Ягодичный мост","2×8–10",difficulty),
                    TrainingExercise("Подъёмы на носки","2×10",difficulty)
                )
                1 -> listOf(
                    TrainingExercise("Приседания с весом тела","2×10–12",difficulty),
                    TrainingExercise("Выпады вперёд","2×8–10/нога",difficulty),
                    TrainingExercise("Ягодичный мост","2×10–12",difficulty),
                    TrainingExercise("Подъёмы на носки","2×12–15",difficulty)
                )
                2 -> listOf(
                    TrainingExercise("Приседания с весом тела","3×8–12",difficulty),
                    TrainingExercise("Выпады вперёд","3×8–10/нога",difficulty),
                    TrainingExercise("Ягодичный мост","3×10–15",difficulty),
                    TrainingExercise("Подъёмы на носки","3×12–15",difficulty)
                )
                3 -> listOf(
                    TrainingExercise("Гоблет-приседания","3×8–10",difficulty),
                    TrainingExercise("Болгарские сплит-приседания","3×8–10/нога",difficulty),
                    TrainingExercise("Ягодичный мост с весом","3×10–12",difficulty),
                    TrainingExercise("Подъёмы на носки","3×12–15",difficulty)
                )
                4 -> listOf(
                    TrainingExercise("Гоблет-приседания тяжёлые","4×6–10",difficulty),
                    TrainingExercise("Болгарские сплит-приседания","4×8–10/нога",difficulty),
                    TrainingExercise("Ягодичный мост с весом","4×8–12",difficulty),
                    TrainingExercise("Подъёмы на носки","4×12–15",difficulty)
                )
                else -> listOf(
                    TrainingExercise("Приседания со штангой","4×5–8",difficulty),
                    TrainingExercise("Болгарские сплит-приседания","4×6–10/нога",difficulty),
                    TrainingExercise("Ягодичный мост с весом","4×8–10",difficulty),
                    TrainingExercise("Подъёмы на носки","4×12–15",difficulty)
                )
            }
            "Выносливость" -> when(tier){
                0 -> listOf(
                    TrainingExercise("Ходьба — разминка","5 мин",difficulty),
                    TrainingExercise("Лёгкий бег","10 мин",difficulty),
                    TrainingExercise("Ходьба — заминка","5 мин",difficulty)
                )
                1 -> listOf(
                    TrainingExercise("Ходьба — разминка","5 мин",difficulty),
                    TrainingExercise("Лёгкий бег","15 мин",difficulty),
                    TrainingExercise("Ходьба — заминка","5 мин",difficulty)
                )
                2 -> listOf(
                    TrainingExercise("Ходьба — разминка","5 мин",difficulty),
                    TrainingExercise("Лёгкий бег","20–30 мин",difficulty),
                    TrainingExercise("Ходьба — заминка","5 мин",difficulty)
                )
                3 -> listOf(
                    TrainingExercise("Лёгкая ходьба — разминка","5 мин",difficulty),
                    TrainingExercise("Бег с ровным темпом","25–35 мин",difficulty),
                    TrainingExercise("Ходьба — заминка","5 мин",difficulty)
                )
                4 -> listOf(
                    TrainingExercise("Лёгкая ходьба — разминка","5 мин",difficulty),
                    TrainingExercise("Бег с интервалами","30–40 мин",difficulty),
                    TrainingExercise("Ходьба — заминка","5–10 мин",difficulty)
                )
                else -> listOf(
                    TrainingExercise("Динамическая разминка","7 мин",difficulty),
                    TrainingExercise("Бег с интервалами","35–45 мин",difficulty),
                    TrainingExercise("Ходьба — заминка","7–10 мин",difficulty)
                )
            }
            else -> emptyList()
        }

    // ---- Exercise catalog (shared/src/commonMain/composeResources/files/exercises.txt) ----
    private var exerciseCatalog: Map<String, List<List<Pair<String,String>>>> = emptyMap()

    fun loadExerciseCatalog(text:String){
        val result=mutableMapOf<String,MutableList<MutableList<Pair<String,String>>>>()
        var group=""; var tier=-1
        text.lineSequence().map{it.trim().removePrefix("\uFEFF")}.forEach{ line->
            if(line.isBlank()||line.startsWith("#")) return@forEach
            if(line.startsWith("[")&&line.endsWith("]")){
                group=line.substring(1,line.length-1).trim(); tier=-1
                result.getOrPut(group){MutableList(6){mutableListOf<Pair<String,String>>()}}
                return@forEach
            }
            if(line.startsWith("@")){ tier=line.drop(1).trim().toIntOrNull()?:-1; return@forEach }
            if(group.isBlank()||tier !in 0..5) return@forEach
            val parts=line.split("|",limit=2)
            val name=parts[0].trim()
            if(name.isBlank()) return@forEach
            result.getValue(group)[tier].add(name to parts.getOrNull(1)?.trim().orEmpty())
        }
        exerciseCatalog=result.mapValues{ e -> e.value.map{ it.toList() } }
        trainingUiRevision++
        invalidateTrainingCache()
    }

    private fun catalogExercises(group:String,tier:Int):List<Pair<String,String>>? =
        exerciseCatalog[group]?.getOrNull(tier)?.takeIf{it.isNotEmpty()}

    /** Number of generated exercises per day for a group; does not depend on level. */
    private fun trainingBaseCount(group:String):Int {
        val sizes=exerciseCatalog[group]?.map{it.size}?.filter{it>0}
        if(!sizes.isNullOrEmpty()) return sizes.min()
        return if(group=="Выносливость") 3 else 4
    }

    private fun visibleGeneratedIndices(group:String,date:String):List<Int> =
        (0 until trainingBaseCount(group)).filter{ !data.training_hidden.contains(trainingKey(date,group,it)) }

    var selectedExerciseKey by mutableStateOf("")

    fun deleteTrainingExercise(ex:TrainingExercise){
        if(ex.isCustom){
            val id=ex.key.removePrefix("CUSTOM|")
            val c=data.custom_trainings.firstOrNull{it.id==id} ?: return
            if(!canExecuteTraining(c.date)) return
            data=data.copy(custom_trainings=data.custom_trainings.filterNot{it.id==id}.toMutableList())
        } else {
            val date=ex.key.substringBefore('|')
            if(!canExecuteTraining(date)) return
            val hidden=data.training_hidden.toMutableList()
            if(!hidden.contains(ex.key)) hidden.add(ex.key)
            data=data.copy(training_hidden=hidden,training_completions=data.training_completions.filterNot{it==ex.key}.toMutableList())
        }
        selectedExerciseKey=""
        trainingUiRevision++
        invalidateTrainingCache()
        save()
    }

    /** Loads "Название 15x3" lines from a .txt program into the selected day. Returns how many were added. */
    fun importTrainingText(group:String,date:String,text:String):Int {
        if(group.isBlank()) return 0
        if(!canExecuteTraining(date)) return 0
        var count=0
        val rx=Regex("^(.*?)[\\s:–—-]*(\\d+\\s*[xXхХ×]\\s*\\d+(?:\\s*[–-]\\s*\\d+)?)\\s*$")
        val added=mutableListOf<CustomTrainingExercise>()
        text.lineSequence().map{it.trim().removePrefix("\uFEFF")}
            .filter{it.isNotBlank()&&!it.startsWith("#")&&!(it.startsWith("[")&&it.endsWith("]"))}
            .forEach{ line->
                var name=line; var presc=""
                if(line.contains("|")){
                    name=line.substringBefore("|").trim(); presc=line.substringAfter("|").trim()
                } else {
                    val m=rx.find(line)
                    if(m!=null){ name=m.groupValues[1].trim().trimEnd(':','-','–','—',' '); presc=m.groupValues[2].replace(Regex("\\s+"),"").replace('x','×').replace('X','×').replace('х','×').replace('Х','×') }
                }
                if(name.isBlank()) name=line
                val id="${Clock.System.now().epochSeconds}_${count}_${name.hashCode()}"
                added.add(CustomTrainingExercise(id=id,date=date,muscle_group=group,name=name.take(80),description="",done=false,created_at=id,prescription=presc))
                count++
            }
        if(count>0){
            data=data.copy(custom_trainings=(data.custom_trainings+added).toMutableList())
            trainingUiRevision++
            invalidateTrainingCache()
            save()
        }
        return count
    }

    fun trainingPlan(group:String,date:String):List<TrainingExercise> {
        val level=muscleLevel(group)
        val tier=tierForLevel(level)
        val difficulty=trainingDifficultyForLevel(level)
        val weekday=LocalDate.parse(date).dayOfWeek.ordinal
        val fromFile=catalogExercises(group,tier)?.map{TrainingExercise(it.first,it.second,difficulty)}
        val baseList=(fromFile ?: builtinTrainingBase(group,tier,difficulty,weekday)).take(trainingBaseCount(group))
        return baseList.mapIndexed { index, ex ->
            ex.copy(
                prescription=weeklyProgressedPrescription(ex.name,ex.prescription,trainingProgressWeek(group)),
                description=trainingDescriptionFor(ex.name),
                key=trainingKey(date,group,index),
                isCustom=false
            )
        }.filterNot{ data.training_hidden.contains(it.key) } + customTrainingExercises(group,date).map {
            TrainingExercise(
                name=it.name,
                prescription=it.prescription.ifBlank{"Пользовательская тренировка"},
                difficulty=trainingDifficultyForGroup(group,date),
                description=it.description,
                key=customTrainingKey(it.id),
                isCustom=true
            )
        }
    }

    fun markReadAll(){data.read_notifications=data.notifications.toMutableList();data=data.copy();save()}
    fun markRead(n:String){if(!data.read_notifications.contains(n)){data.read_notifications.add(n);data=data.copy();save()}}
    fun onNotificationClicked(n:String){
        markRead(n)
        val task = data.tasks.firstOrNull { n.contains(it.text, ignoreCase = false) }
        if(task != null){
            activeTab = "ЗАДАЧИ"
            selectedTaskDate = task.date
            taskView = if(task.done) "DONE" else "ACTIVE"
            focusedTask = task
            notificationsOpen = false
        }
    }
    fun levelNeed(level:Int):Int {
        var target = 100.0
        // Exponential growth quickly exceeds Int range. Saturate then instead of
        // continuing a potentially huge repeat loop for damaged profile values.
        repeat(level.coerceIn(0, 80)) {
            target = kotlin.math.ceil(target * 1.25)
            if (target >= Int.MAX_VALUE.toDouble()) return Int.MAX_VALUE
        }
        return target.toInt().coerceAtLeast(1)
    }
    private fun totalXp():Long {
        var total=0L
        if(data.level>0){
            for(l in 0 until data.level) total += levelNeed(l).toLong()
        }
        return total + data.level_xp.toLong().coerceAtLeast(0L)
    }
    private fun setTotalXp(totalInput:Long){
        var total=totalInput.coerceAtLeast(0L)
        var lvl=0
        var need=levelNeed(0).toLong()
        while(total>=need && lvl < 10_000){
            total-=need
            lvl++
            need=levelNeed(lvl).toLong()
        }
        data.level=lvl
        data.level_xp=total.toInt()
        if(lvl>=2&&!data.achievements.any{it.id=="first"})data.achievements.add(Achievement("first","Первопроходец","Достигнут 2 уровень"))
    }
    fun addXp(amount:Int){
        setTotalXp(totalXp()+amount.toLong())
        data=data.copy()
    }
    fun addAdminXp(amount:Int){
        if(data.role=="Системный администратор"){
            addXp(amount)
            save()
        }
    }
    fun addAdminRep(amount:Int){
        if(data.role=="Системный администратор"){
            val next=maxOf(0,data.reputation_xp+amount)
            data=data.copy(reputation_xp=next)
            save()
        }
    }
    fun complete(t:TaskItem){if(!t.done){t.done=true;t.completed_date=appTodayIso();t.xp_awarded_amount=t.points;addXp(t.points);data=data.copy();save()}}
    fun restore(t:TaskItem){if(t.done){val awarded=if(t.xp_awarded_amount>0)t.xp_awarded_amount else t.points;t.done=false;t.completed_date="";t.xp_awarded_amount=0;addXp(-awarded);data=data.copy();save()}}
    fun delete(t:TaskItem){if(t.done){val awarded=if(t.xp_awarded_amount>0)t.xp_awarded_amount else t.points;addXp(-awarded)};data.tasks.remove(t);data=data.copy();save()}
    fun parseTime(s:String):Int{val x=s.trim().uppercase();val parts=x.removeSuffix("AM").removeSuffix("PM").trim().split(":");var h=parts[0].toIntOrNull()?:8;val m=parts.getOrNull(1)?.trim()?.toIntOrNull()?:0;if(x.contains("PM")&&h<12)h+=12;if(x.contains("AM")&&h==12)h=0;return h*60+m}
    private fun endTime(t:TaskItem)=parseTime(t.start_time)+t.duration_minutes
    fun conflicts(t:TaskItem,ignore:TaskItem?=null)=data.tasks.filter{it!==ignore&&!it.done&&it.date==t.date&&parseTime(it.start_time)<endTime(t)&&parseTime(t.start_time)<endTime(it)}
    fun openCreate(){editorMode=EditorMode.CREATE;editorTask=null}
    fun openEdit(t:TaskItem){editorMode=EditorMode.EDIT;editorTask=t}
    fun closeEditor(){editorMode=null;editorTask=null}
    fun openTrainingCreator(group:String,date:String){
        if(group.isBlank() || !canExecuteTraining(date)) return
        trainingCreatorGroup=group
        trainingCreatorDate=date
        trainingCreatorOpen=true
    }
    fun closeTrainingCreator(){trainingCreatorOpen=false}
    fun addCustomTraining(group:String,date:String,name:String,description:String){
        if(group.isBlank() || name.isBlank()) return
        if(!canExecuteTraining(date)) return
        val id="${Clock.System.now().epochSeconds}_${name.hashCode()}_${data.custom_trainings.size}"
        data.custom_trainings.add(CustomTrainingExercise(id=id,date=date,muscle_group=group,name=name,description=description,done=false,created_at=id))
        data=data.copy(custom_trainings=data.custom_trainings.toMutableList())
        trainingCreatorOpen=false
        trainingUiRevision++
        save()
    }
    fun addTask(text:String,description:String,time:String,duration:Int,diff:String,date:String,remind:Boolean,muscleGroup:String,instruction:String){
        val t=TaskItem(
            text=text,start_time=time,date=date,duration_minutes=duration,difficulty=diff,
            points=points(diff),level_xp=points(diff),reputation_xp=0,reputation_rank=0,
            done=false,penalized=false,hard_missed=false,remind=remind,remind_sent=false,
            start_notification_sent=false,description=description,muscle_group=muscleGroup,instruction=instruction,created_date=appTodayIso()
        )
        if(conflicts(t).isNotEmpty()){
            closeEditor()
            dialog=DialogState("Пересечение задач","Задача пересекается с другой задачей. Создать несмотря на пересечение?",{data.tasks.add(t);data=data.copy();afterAdd(t)})
        } else {
            data.tasks.add(t)
            data=data.copy()
            afterAdd(t)
        }
    }
    private fun afterAdd(t:TaskItem){selectedTaskDate=t.date;taskView="ACTIVE";focusedTask=t;data=data.copy();save();closeEditor()}
    fun editTask(t:TaskItem,text:String,description:String,time:String,duration:Int,diff:String,date:String,remind:Boolean,muscleGroup:String,instruction:String){
        val preservedGroup=if(muscleGroup.isBlank()) t.muscle_group else muscleGroup
        val candidate=t.copy(text=text,description=description,start_time=time,date=date,duration_minutes=duration,difficulty=diff,points=points(diff),level_xp=points(diff),remind=remind,start_notification_sent=false,muscle_group=preservedGroup,instruction=instruction)
        val apply={
            val wasDone=t.done
            val oldAward=t.xp_awarded_amount
            t.text=candidate.text;t.description=candidate.description;t.start_time=candidate.start_time;t.date=candidate.date;t.duration_minutes=candidate.duration_minutes;t.difficulty=candidate.difficulty;t.points=candidate.points;t.level_xp=candidate.level_xp;t.remind=candidate.remind;t.start_notification_sent=false;t.remind_sent=false;t.muscle_group=candidate.muscle_group;t.instruction=candidate.instruction
            if(wasDone){
                val newAward=if(oldAward>0)oldAward else t.points
                if(candidate.points!=newAward){addXp(candidate.points-newAward);t.xp_awarded_amount=candidate.points}
            }
            selectedTaskDate=date;data=data.copy();save();closeEditor()
        }
        if(conflicts(candidate,t).isNotEmpty()){
            closeEditor()
            dialog=DialogState("Пересечение задач","Изменение создаёт пересечение. Применить всё равно?",apply)
        } else apply()
    }
    fun toggleWorkMark(date:String,mark:String){if(mark.isBlank())data.calendar_marks.remove(date) else data.calendar_marks[date]=mark;data=data.copy();save()}
    fun updateProfile(name:String,login:String,password:String){val old=username;if(login.isNotBlank()&&login!=old){users.remove(old);username=login};data.display_name=name;if(password.isNotBlank())data.password=password;users[username]=data;data=data.copy();save()}
    fun setBackground(path:String){data.background=path;data=data.copy();save()}
    fun setPhoto(path:String){data.photo=path;data=data.copy();save()}
    fun resetBackground(){data.background="";data=data.copy();save()}
    fun logout(){
        settingsOpen=false
        val oldUser = username
        if(oldUser.isNotBlank()) {
            users[oldUser] = data
        }
        username=""
        data=UserData()
        save()
    }
    fun setAppearance(bgOpacity:Double,blockOpacity:Double,blur:Double){data.background_opacity=bgOpacity;data.block_opacity=blockOpacity;data.background_blur=blur;data=data.copy();save()}
    fun setNotifications(enabled:Boolean){data.notifications_enabled=enabled;data=data.copy();save()}
    fun tick(){
        val now=appNow()
        var changed=false
        val today = now.date
        if(lastPenaltyCheckDate != today.toString()) {
            lastPenaltyCheckDate = today.toString()
            if(username.isNotBlank()) applyTrainingInactivityPenalties()
        }
        val tomorrow = run {
            val month = today.month.ordinal + 1
            val lastDay = daysInMonth(today.year, month)
            if (today.day < lastDay) {
                LocalDate(today.year, month, today.day + 1)
            } else if (month < 12) {
                LocalDate(today.year, month + 1, 1)
            } else {
                LocalDate(today.year + 1, 1, 1)
            }
        }
        data.tasks.filter{!it.done}.forEach{ task ->
            if(!data.notifications_enabled) return@forEach
            if(task.date == today.toString()){
                val start=parseTime(task.start_time)
                val cur=now.hour*60+now.minute
                if(!task.start_notification_sent&&start-cur in 0..15){
                    val text="Через ${start-cur} мин: ${task.text}"
                    if(!data.notifications.contains(text)) data.notifications.add(text)
                    task.start_notification_sent=true
                    changed=true
                }
            }
            if(task.remind && task.date == tomorrow.toString() && !task.remind_sent){
                val text="Завтра в ${task.start_time}: ${task.text}"
                if(!data.notifications.contains(text)) data.notifications.add(text)
                task.remind_sent=true
                changed=true
            }
        }
        if(changed){data=data.copy();save()}
    }
}

@Composable private fun MainScreen(vm:LifeViewModel, onLogout:()->Unit){
    LaunchedEffect(Unit){while(true){vm.tick();delay(30.seconds)}}
    BoxWithConstraints(Modifier.fillMaxSize().background(Black)){
        val isPhone = this.maxWidth < 700.dp
        UserBackground(vm)
        if (vm.settingsOpen) {
            SettingsDialog(vm, onLogout)
        } else {
            if (isPhone) {
                MobileMainLayout(vm)
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal=36.dp,vertical=14.dp)){
                    Header(vm)
                    Spacer(Modifier.height(12.dp))
                    MainContent(vm)
                }
            }
        }
        if (vm.menuOpen) {
            NavigationDrawer(vm, if (isPhone) 54.dp else 58.dp)
            // Keep the menu control fixed in its header position while the drawer
            // is open; its three bars transform into an X and remain clickable.
            val menuX = if (isPhone) when {
                maxWidth < 320.dp -> 7.dp
                maxWidth < 360.dp -> 9.dp
                else -> 12.dp
            } else 36.dp
            val menuY = if (isPhone) 8.dp else 14.dp
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.offset(x = menuX, y = menuY)) {
                    MenuButton(if (isPhone) 54.dp else 58.dp, isOpen = true) { vm.menuOpen = false }
                }
            }
        }
    }
    if (vm.avatarEditorOpen) AvatarEditDialog(vm)
    vm.dialog?.let{d->ConfirmDialog(d,vm)}
    vm.editorMode?.let{TaskEditor(vm,it)}
    if(vm.notificationsOpen) NotificationCenterDialog(vm)
    if(vm.timeTravelOpen) TimeTravelDialog(vm)
    if(vm.trainingCreatorOpen) TrainingCreatorDialog(vm)
}

@Composable
private fun MenuButton(buttonSize: Dp, isOpen: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(buttonSize * 0.85f)
            .clip(RoundedCornerShape(12.dp))
            .then(if (isOpen) Modifier else Modifier.background(Color(0xE6050B10)))
            .then(if (isOpen) Modifier else Modifier.border(BorderStroke(2.dp, Green), RoundedCornerShape(12.dp)))
            .clickable(onClick = onClick)
            // Slightly larger inner padding reduces the three-line/X glyph by 10%.
            .padding(horizontal = buttonSize * 0.198f, vertical = buttonSize * 0.223f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.8.dp.toPx()
            if (isOpen) {
                drawLine(
                    color = Green,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.18f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.82f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Green,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.18f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.82f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            } else {
                for (fraction in listOf(0.18f, 0.50f, 0.82f)) {
                    drawLine(
                        color = Green,
                        start = androidx.compose.ui.geometry.Offset(0f, this.size.height * fraction),
                        end = androidx.compose.ui.geometry.Offset(this.size.width, this.size.height * fraction),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(vm: LifeViewModel, size: Dp) {
    val avatar = remember(vm.data.photo) {
        if (vm.data.photo.isBlank()) null else loadPlatformImage(vm.data.photo)
    }
    Box(Modifier.size(size)) {
        if (avatar != null) {
            Image(avatar, null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Image(painterResource(Res.drawable.logo), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.align(Alignment.BottomEnd)
                .offset(x = 3.dp, y = 3.dp)
                .size(23.dp)
                .clip(CircleShape)
                .background(Color(0xFF020702))
                .clickable { vm.avatarEditorOpen = true },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(Res.drawable.a1),
                contentDescription = "Редактировать аватарку",
                modifier = Modifier.fillMaxSize().padding(1.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun NavigationDrawer(vm: LifeViewModel, logoSize: Dp) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isPhone = maxWidth < 700.dp
        val density = LocalDensity.current
        val notchShift = with(density) { 15.toDp() }
        val notchStart = (if (isPhone) 51.dp else 78.dp) + notchShift
        // Keep the horizontal cut-out edge below avatar/time, but just above the stat cards.
        val notchHeight = if (isPhone) 61.dp else 73.dp
        val notchStartPx = with(density) { notchStart.toPx() }
        val notchHeightPx = with(density) { notchHeight.toPx() }
        val cornerPx = with(density) { 12.dp.toPx() }

        // Darken the working area below the header. The top-right notch is left
        // clear so avatar, nickname, role and time remain visible behind the menu.
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(notchHeight))
            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable { vm.menuOpen = false }
            )
        }
        // Consume taps in the transparent header cut-out without dimming it.
        Box(
            Modifier.offset(x = notchStart)
                .width((maxWidth - notchStart).coerceAtLeast(0.dp))
                .height(notchHeight)
        )

        val drawerWidth = if (maxWidth < 400.dp) maxWidth * 0.88f else 340.dp
        val drawerShape = GenericShape { size, _ ->
            val corner = cornerPx.coerceAtMost(size.height / 4f)
            moveTo(0f, 0f)
            lineTo((notchStartPx - corner).coerceAtLeast(0f), 0f)
            quadraticTo(notchStartPx, 0f, notchStartPx, corner)
            lineTo(notchStartPx, (notchHeightPx - corner).coerceAtLeast(corner))
            quadraticTo(notchStartPx, notchHeightPx, notchStartPx + corner, notchHeightPx)
            lineTo((size.width - corner).coerceAtLeast(notchStartPx + corner), notchHeightPx)
            quadraticTo(size.width, notchHeightPx, size.width, notchHeightPx + corner)
            lineTo(size.width, (size.height - corner).coerceAtLeast(notchHeightPx + corner))
            quadraticTo(size.width, size.height, size.width - corner, size.height)
            lineTo(corner, size.height)
            quadraticTo(0f, size.height, 0f, size.height - corner)
            close()
        }
        Surface(
            modifier = Modifier.width(drawerWidth).fillMaxHeight().align(Alignment.CenterStart)
                .border(BorderStroke(2.dp, Green), drawerShape)
                // Consume clicks silently: no ripple, so tapping the panel, logo
                // or any empty spot no longer makes the menu blink.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
            color = Color(0xF2050B10),
            shape = drawerShape
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxHeight().width(3.dp).background(Green))
                Column(
                    Modifier.fillMaxHeight().weight(1f).padding(start=16.dp, end=16.dp, top=notchHeight + 8.dp, bottom=14.dp)
                ) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        DrawerItem("ЗАДАЧИ", vm.activeTab == "ЗАДАЧИ", large = false, clickable = false) { }
                        DrawerItem("СПИСОК ЗАДАЧ", vm.activeTab == "ЗАДАЧИ", large = true) { selectDrawerTab(vm, "ЗАДАЧИ") }
                        DrawerItem("ЗАМЕТКИ", vm.activeTab == "ЗАМЕТКИ", large = true) { selectDrawerTab(vm, "ЗАМЕТКИ") }
                        DrawerItem("СПИСОК ЗАМЕТОК", vm.activeTab == "СПИСОК ЗАМЕТОК", large = true) { selectDrawerTab(vm, "СПИСОК ЗАМЕТОК") }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF245B25), thickness = 1.dp)
                        DrawerItem("ПЛАНЫ", vm.activeTab.startsWith("ПЛАН"), large = false, clickable = false) { }
                        DrawerItem("КАРТА ПЛАНОВ", vm.activeTab == "КАРТА ПЛАНОВ", large = true) { selectDrawerTab(vm, "КАРТА ПЛАНОВ") }
                        DrawerItem("ПЛАНЫ В ПОДРОБНОСТЯХ", vm.activeTab == "ПЛАНЫ В ПОДРОБНОСТЯХ", large = true) { selectDrawerTab(vm, "ПЛАНЫ В ПОДРОБНОСТЯХ") }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF245B25), thickness = 1.dp)
                        DrawerItem("ЗДОРОВЬЕ", vm.activeTab == "ЗДОРОВЬЕ", large = false, clickable = false) { }
                        DrawerItem("ТРЕНИРОВКИ", vm.healthTab == "ТРЕНИРОВКИ" && vm.activeTab == "ЗДОРОВЬЕ", large = true) { selectDrawerTab(vm, "ТРЕНИРОВКИ") }
                        DrawerItem("ПИТАНИЕ", vm.healthTab == "ПИТАНИЕ" && vm.activeTab == "ЗДОРОВЬЕ", large = true) { selectDrawerTab(vm, "ПИТАНИЕ") }
                        DrawerItem("СОН", vm.healthTab == "СОН" && vm.activeTab == "ЗДОРОВЬЕ", large = true) { selectDrawerTab(vm, "СОН") }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF245B25), thickness = 1.dp)
                        DrawerItem("ФИНАНСЫ", vm.activeTab == "ФИН. РАСЧЕТ", large = false, clickable = false) { }
                        DrawerItem("ФИН. РАСЧЕТ", vm.activeTab == "ФИН. РАСЧЕТ", large = true) { selectDrawerTab(vm, "ФИН. РАСЧЕТ") }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF245B25), thickness = 1.dp)
                        DrawerItem("СТАТИСТИКА", vm.activeTab == "СТАТИСТИКА", large = false) { selectDrawerTab(vm, "СТАТИСТИКА") }
                        DrawerItem("ГРАФИКИ", vm.activeTab == "ГРАФИКИ", large = true) { selectDrawerTab(vm, "ГРАФИКИ") }
                        DrawerItem("ДОСТИЖЕНИЯ", vm.activeTab == "ДОСТИЖЕНИЯ", large = true) { selectDrawerTab(vm, "ДОСТИЖЕНИЯ") }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Green, thickness = 2.dp)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(Res.drawable.logo),
                            contentDescription = "Логотип DigitalLifeSystem",
                            modifier = Modifier.size(logoSize)
                        )
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.scale(2.5f)) {
                        OutlineIconButton(IconKind.GEAR, "") {
                            vm.menuOpen = false
                            vm.settingsOpen = true
                        }
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, selected: Boolean, large: Boolean, clickable: Boolean = true, onClick: () -> Unit) {
    val itemShape = RoundedCornerShape(5.dp)
    val protected = label in listOf("ЗАДАЧИ", "ПЛАНЫ", "ЗДОРОВЬЕ", "ФИНАНСЫ", "СТАТИСТИКА")
    Row(
        Modifier.fillMaxWidth()
            .offset(x = if (protected) 0.dp else 20.dp)
            .padding(bottom = 3.dp)
            .clip(itemShape)
            .background(if (selected) Color(0xFF0B1B0B) else Color.Transparent)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = if (large) 3.dp else 5.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(3.dp).height(if (large) 25.dp else 22.dp)
                .background(if (selected) Green else Color.Transparent)
        )
        Spacer(Modifier.width(if (large) 8.dp else 11.dp))
        Text(
            label,
            color = if (selected) Green else Light,
            fontFamily = FontFamily.Serif,
            fontSize = if (large) 9.sp else 14.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun selectDrawerTab(vm: LifeViewModel, tab: String) {
    when (tab) {
        "ТРЕНИРОВКИ", "ПИТАНИЕ", "СОН" -> {
            vm.activeTab = "ЗДОРОВЬЕ"
            vm.healthTab = tab
        }
        "НАСТРОЙКИ" -> vm.settingsOpen = true
        else -> vm.activeTab = tab
    }
    vm.selectedTaskDate = todayIso()
    vm.selectedWorkDate = todayIso()
    vm.selectedTrainingDate = todayIso()
    if (tab != "ЗАДАЧИ") vm.taskView = "ACTIVE"
    vm.menuOpen = false
}

@Composable
private fun AvatarEditDialog(vm: LifeViewModel) {
    val avatar = remember(vm.data.photo) {
        if (vm.data.photo.isBlank()) null else loadPlatformImage(vm.data.photo)
    }
    AlertDialog(
        onDismissRequest = { vm.avatarEditorOpen = false },
        containerColor = Color(0xFF050B10),
        title = { Text("Редактирование аватарки", color = Green, fontFamily = blenderBold()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (avatar != null) {
                    Image(avatar, null, Modifier.size(112.dp).clip(CircleShape).border(2.dp, Green, CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Image(painterResource(Res.drawable.logo), null, Modifier.size(112.dp).clip(CircleShape).border(2.dp, Green, CircleShape), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        pickImage {
                            vm.setPhoto(it)
                            vm.avatarEditorOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF071507)),
                    border = BorderStroke(1.dp, Green)
                ) { Text("Выбрать изображение", color = White, fontFamily = blenderBold()) }
                if (vm.data.photo.isNotBlank()) {
                    TextButton(onClick = { vm.setPhoto("") }) {
                        Text("Вернуть логотип", color = Light, fontFamily = blenderBook())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.avatarEditorOpen = false }) {
                Text("Готово", color = Green, fontFamily = blenderBold())
            }
        }
    )
}

@Composable
private fun StatisticsPanel(vm:LifeViewModel){
    val items=listOf(
        "ЛЕГКИЕ" to vm.completedCount("Легкий"),
        "СРЕДНИЕ" to vm.completedCount("Средний"),
        "СЛОЖНЫЕ" to vm.completedCount("Тяжелый"),
        "ЭКСТРЕМАЛЬНЫЕ" to vm.completedCount("Экстремальный")
    )
    Surface(
        Modifier.fillMaxWidth(),
        color=Color(0xD9050B10),
        border=BorderStroke(2.dp,Color(0xFF134E2C)),
        shape=RoundedCornerShape(10.dp)
    ){
        Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.width(4.dp).height(18.dp).background(Green))
                Spacer(Modifier.width(7.dp))
                Text("СТАТИСТИКА",color=Green,fontFamily=blenderBold(),fontSize=15.sp)
                Spacer(Modifier.width(8.dp))
                Text("ВЫПОЛНЕННЫЕ ЗАДАЧИ",color=Color.Gray,fontFamily=blenderBook(),fontSize=9.sp)
            }
            Spacer(Modifier.height(6.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()){
                if(maxWidth < 560.dp){
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            StatisticsItem(items[0].first,items[0].second,Modifier.weight(1f))
                            StatisticsItem(items[1].first,items[1].second,Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            StatisticsItem(items[2].first,items[2].second,Modifier.weight(1f))
                            StatisticsItem(items[3].first,items[3].second,Modifier.weight(1f))
                        }
                    }
                }else{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        items.forEach{(label,count)->StatisticsItem(label,count,Modifier.weight(1f))}
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsItem(label:String,count:Int,modifier:Modifier=Modifier){
    val accent = when(label) {
        "ЛЕГКИЕ" -> Color(0xFF7EFF96)
        "СРЕДНИЕ" -> Color(0xFFE7F000)
        "СЛОЖНЫЕ" -> Color(0xFFFFA000)
        else -> Color(0xFFFF3B30)
    }
    val background = when(label) {
        "ЛЕГКИЕ" -> Color(0xFF06150D)
        "СРЕДНИЕ" -> Color(0xFF171800)
        "СЛОЖНЫЕ" -> Color(0xFF1A1000)
        else -> Color(0xFF190806)
    }
    Surface(modifier.height(42.dp).clip(RoundedCornerShape(7.dp)),color=background,border=BorderStroke(1.dp,accent.copy(alpha=.78f)),shape=RoundedCornerShape(7.dp)){
        Row(Modifier.fillMaxSize().padding(horizontal=9.dp),verticalAlignment=Alignment.CenterVertically){
            Text(label,color=accent,fontFamily=blenderBook(),fontSize=9.sp)
            Spacer(Modifier.weight(1f))
            Text(count.toString(),color=accent,fontFamily=blenderBold(),fontSize=16.sp)
        }
    }
}

@Composable private fun UserBackground(vm:LifeViewModel){val custom=remember(vm.data.background){if(vm.data.background.isBlank())null else loadPlatformImage(vm.data.background)};if(custom!=null)Image(custom,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alpha=vm.data.background_opacity.toFloat()) else Image(painterResource(Res.drawable.fon),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alpha=vm.data.background_opacity.toFloat())}


@Composable
private fun MobileMainLayout(vm:LifeViewModel){
    BoxWithConstraints(Modifier.fillMaxSize()){
        val compact=maxWidth < 360.dp
        val veryCompact=maxWidth < 320.dp
        val sidePadding=when{ veryCompact -> 7.dp; compact -> 9.dp; else -> 12.dp }
        val topGap=when{ veryCompact -> 1.dp; else -> 2.dp }
        Column(Modifier.fillMaxSize()){
            MobileHeader(vm, sidePadding)
            Spacer(Modifier.height(topGap))
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal=sidePadding)){
                MobileMainContent(vm)
            }
        }
    }
}

@Composable
private fun MobileHeader(vm:LifeViewModel, sidePadding: Dp){
    Column(Modifier.fillMaxWidth().padding(horizontal=sidePadding, vertical=8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
            MenuButton(54.dp, isOpen = vm.menuOpen) { vm.menuOpen = !vm.menuOpen }
            Spacer(Modifier.width(8.dp))
            ProfileAvatar(vm, 48.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)){
                Text(vm.data.display_name.ifBlank{vm.username},color=White,fontFamily=blenderBold(),fontSize=16.sp,maxLines=1)
                Text("Роль: ${vm.data.role}",color=Light,fontFamily=blenderBook(),fontSize=10.sp,maxLines=1)
                CurrentTimeText(vm,color=Color.Gray,fontSize=9.sp,onClick={vm.timeTravelOpen=true})
            }
            val unread = vm.unread()
            OutlineIconButton(IconKind.BELL, if(unread>0)unread.toString() else "") { vm.notificationsOpen=true }
        }
        Spacer(Modifier.height(8.dp))
        // Match the horizontal inset of content panels: adaptive screen margin + 12dp.
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            LevelWaterCard(vm, Modifier.weight(1f))
            ReputationCard(vm, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun LevelWaterCard(vm:LifeViewModel, modifier:Modifier = Modifier){
    val need = vm.levelNeed(vm.data.level).coerceAtLeast(1)
    val target = (vm.data.level_xp.toFloat()/need.toFloat()).coerceIn(0f,1f)
    val fill by animateFloatAsState(target, animationSpec=tween(900), label="level-water")
    val borderColor = Color(0xFF3C8FBE)
    val valueColor = Color(0xFF123A64)
    Box(modifier.height(62.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xE6071522)).border(2.dp,borderColor,RoundedCornerShape(10.dp))){
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(fill).align(Alignment.BottomCenter)
                .background(Color(0xFF93DFFF).copy(alpha=0.62f))
        )
        Column(Modifier.padding(horizontal=10.dp,vertical=5.dp)) {
            Text("LVL",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=10.sp)
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(vm.data.level.toString(),color=valueColor,fontFamily=blenderBold(),fontSize=20.sp)
                if(vm.data.role=="Системный администратор"){
                    Spacer(Modifier.width(6.dp))
                    AdminXpControls(onPlus={vm.addAdminXp(100)},onMinus={vm.addAdminXp(-100)})
                }
            }
            Text("XP ${vm.data.level_xp}/${need}",color=Color(0xFFB9EAFF),fontFamily=blenderBook(),fontSize=8.sp)
        }
    }
}

@Composable
private fun ReputationCard(vm:LifeViewModel, modifier:Modifier = Modifier){
    Box(modifier.height(62.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xE6051008)).border(2.dp,Color(0xFF2D7B3E),RoundedCornerShape(10.dp))){
        Column(Modifier.padding(horizontal=10.dp,vertical=5.dp)) {
            Text("REP",color=Light,fontFamily=blenderBold(),fontSize=10.sp)
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(vm.data.reputation_xp.toString(),color=Color(0xFF9CFFAD),fontFamily=blenderBold(),fontSize=20.sp)
                if(vm.data.role=="Системный администратор"){
                    Spacer(Modifier.width(6.dp))
                    AdminXpControls(onPlus={vm.addAdminRep(100)},onMinus={vm.addAdminRep(-100)})
                }
            }
            Text("РЕПУТАЦИЯ",color=Color(0xFF9CFFAD),fontFamily=blenderBook(),fontSize=8.sp)
        }
    }
}

@Composable
private fun AdminXpControls(onPlus:()->Unit,onMinus:()->Unit){
    Row(horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.CenterVertically){
        TextButton(onClick=onPlus,modifier=Modifier.size(28.dp),contentPadding=PaddingValues(0.dp)){Text("+",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=17.sp)}
        TextButton(onClick=onMinus,modifier=Modifier.size(28.dp),contentPadding=PaddingValues(0.dp)){Text("−",color=Color(0xFFFF7A7A),fontFamily=blenderBold(),fontSize=17.sp)}
    }
}

@Composable
private fun OutlineIconButton(kind:IconKind,count:String,onClick:()->Unit){
    val controlSize = if(kind==IconKind.GEAR) 36.dp else 47.dp
    Box(Modifier.size(controlSize),contentAlignment=Alignment.Center){
        IconButton(onClick=onClick,modifier=Modifier.size(controlSize)){
            Canvas(Modifier.size(if(kind==IconKind.GEAR) 23.dp else 43.875.dp)){
                val stroke=2.1f
                val c=androidx.compose.ui.geometry.Offset(size.width/2f,size.height/2f)
                when(kind){
                    IconKind.BELL -> {
                        val p=Path().apply{
                            moveTo(size.width*0.24f,size.height*0.72f)
                            lineTo(size.width*0.76f,size.height*0.72f)
                            lineTo(size.width*0.69f,size.height*0.62f)
                            lineTo(size.width*0.69f,size.height*0.42f)
                            cubicTo(size.width*0.69f,size.height*0.20f,size.width*0.31f,size.height*0.20f,size.width*0.31f,size.height*0.42f)
                            lineTo(size.width*0.31f,size.height*0.62f)
                            close()
                        }
                        drawPath(p,color=Green,style=Stroke(cap=StrokeCap.Round,join=StrokeJoin.Round,width=stroke))
                        drawCircle(Green,size.minDimension*0.055f,center=androidx.compose.ui.geometry.Offset(size.width*0.5f,size.height*0.80f))
                    }
                    IconKind.GEAR -> {
                        val outerR=size.minDimension*0.39f
                        val innerR=size.minDimension*0.17f
                        val toothInner=size.minDimension*0.33f
                        val toothOuter=size.minDimension*0.43f
                        val path=Path()
                        for(i in 0 until 16){
                            val a=(-90.0 + i*22.5) * (kotlin.math.PI/180.0)
                            val r=when(i%4){0,1->toothOuter;2,3->toothInner;else->toothInner}
                            val x=c.x+kotlin.math.cos(a).toFloat()*r
                            val y=c.y+kotlin.math.sin(a).toFloat()*r
                            if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
                        }
                        path.close()
                        drawPath(path,color=Green,style=Stroke(cap=StrokeCap.Round,join=StrokeJoin.Round,width=stroke))
                        drawCircle(Green,outerR*0.60f,center=c,style=Stroke(width=stroke))
                        drawCircle(Green,innerR,center=c,style=Stroke(width=stroke))
                    }
                }
            }
        }
        if(count.isNotEmpty()){
            Box(Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = (-5).dp).clip(CircleShape).background(Green).size(15.dp),contentAlignment=Alignment.Center){
                Text(count,color=Black,fontFamily=blenderBold(),fontSize=8.sp)
            }
        }
    }
}
private enum class IconKind { BELL, GEAR }

@Composable
private fun MobileMainContent(vm:LifeViewModel){
    if(vm.activeTab=="СТАТИСТИКА"){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            MobileSectionTitle("СТАТИСТИКА")
            StatisticsPanel(vm)
            OverallTrainingProgressPentagon(vm)
        }
        return
    }
    if(vm.activeTab=="ДОСТИЖЕНИЯ"){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            MobileSectionTitle("ДОСТИЖЕНИЯ")
            AchievementsPanel(vm)
        }
        return
    }
    if(vm.activeTab=="ЗДОРОВЬЕ"){
        HealthPanel(vm)
        return
    }
    when (vm.activeTab) {
        "ЗАМЕТКИ" -> { NotesWorkspace(vm); return }
        "СПИСОК ЗАМЕТОК" -> { NotesListWorkspace(vm); return }
        "ПЛАНЫ В ПОДРОБНОСТЯХ", "ПЛАНЫ" -> { DetailedPlansWorkspace(vm); return }
        "КАРТА ПЛАНОВ" -> { PlansMapWorkspace(vm); return }
        "ФИН. РАСЧЕТ" -> { FinanceWorkspace(vm); return }
        "ГРАФИКИ" -> { FinanceChartsWorkspace(vm); return }
    }
    if(vm.activeTab!="ЗАДАЧИ"){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=4.dp)){
            MobileSectionTitle(vm.activeTab)
            MobilePlaceholder(vm.activeTab)
        }
        return
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=4.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        MobileTaskPanel(vm)
        MobileCalendarPanel(vm)
        MobileWorkload(vm)
    }
}


@Composable
private fun AchievementsPanel(vm:LifeViewModel){
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(1.dp,Grid),shape=RoundedCornerShape(10.dp)){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("ДОСТИЖЕНИЯ",color=Green,fontFamily=blenderBold(),fontSize=20.sp)
            if(vm.data.achievements.isEmpty()){
                Text("Пока нет достижений",color=Color.Gray,fontFamily=blenderBook(),fontSize=12.sp)
            }else{
                vm.data.achievements.forEach{a->
                    Surface(Modifier.fillMaxWidth(),color=Color(0xFF071007),border=BorderStroke(1.dp,Grid),shape=RoundedCornerShape(8.dp)){
                        Column(Modifier.padding(10.dp)){
                            Text(a.name,color=Light,fontFamily=blenderBold(),fontSize=14.sp)
                            if(a.description.isNotBlank()) Text(a.description,color=Color.Gray,fontFamily=blenderBook(),fontSize=10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallTrainingProgressPentagon(vm:LifeViewModel){
    val revision=vm.trainingUiRevision
    val groups=listOf("Грудь","Спина","Ноги","Выносливость","Пресс","Руки и плечи")
    val values=groups.map{ remember(revision,it){ vm.overallTrainingProgress(it).coerceIn(0f,1f) } }
    val levels=groups.map{ vm.muscleLevel(it) }
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(1.dp,Color(0xFF1A5E7A)),shape=RoundedCornerShape(10.dp)){
        Column(Modifier.fillMaxWidth().padding(horizontal=10.dp, vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text("ОБЩИЙ ПРОГРЕСС ТРЕНИРОВОК",color=Green,fontFamily=blenderBold(),fontSize=15.sp)
            Spacer(Modifier.height(3.dp))
            Text("Грудь • Спина • Руки и плечи • Пресс • Ноги • Выносливость",color=Color(0xFF6FA7BF),fontFamily=blenderBook(),fontSize=8.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(320.dp)){
                Canvas(Modifier.fillMaxSize().padding(start=44.dp,end=44.dp,top=38.dp,bottom=42.dp)){
                    val c=androidx.compose.ui.geometry.Offset(size.width/2f,size.height/2f)
                    val r=size.minDimension*0.46f
                    fun point(i:Int,ratio:Float):androidx.compose.ui.geometry.Offset{
                        val a=(-90.0+i*60.0)*(kotlin.math.PI/180.0)
                        return androidx.compose.ui.geometry.Offset(c.x+kotlin.math.cos(a).toFloat()*r*ratio,c.y+kotlin.math.sin(a).toFloat()*r*ratio)
                    }
                    for(levelRatio in listOf(.2f,.4f,.6f,.8f,1f)){
                        val p=Path()
                        for(i in 0..5){val q=point(i,levelRatio);if(i==0)p.moveTo(q.x,q.y) else p.lineTo(q.x,q.y)}
                        p.close()
                        drawPath(p,color=Color(0xFF245D7B),style=Stroke(width=1.1f))
                    }
                    for(i in 0..5) drawLine(Color(0xFF245D7B),c,point(i,1f),strokeWidth=1.0f)
                    val points=values.mapIndexed{i,v->point(i,v)}
                    val fill=Path().apply {
                        points.forEachIndexed{i,q->if(i==0)moveTo(c.x,c.y);lineTo(q.x,q.y)}
                        close()
                    }
                    drawPath(fill,color=Color(0xFF71D7FF).copy(alpha=.32f))
                    drawPath(fill,color=Color(0xFF71D7FF),style=Stroke(width=2.2f))
                    points.forEach { drawCircle(Color(0xFFB9EAFF),3.5f,center=it) }
                }
                Text("ГРУДЬ\n${levels[0]} ур.",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=8.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.align(Alignment.TopCenter).padding(top=0.dp))
                Text("СПИНА\n${levels[1]} ур.",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=8.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.align(Alignment.TopEnd).padding(top=48.dp,end=1.dp))
                Text("НОГИ\n${levels[2]} ур.",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=8.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.align(Alignment.BottomEnd).padding(bottom=48.dp,end=1.dp))
                Text("ВЫНОСЛИВОСТЬ\n${levels[3]} ур.",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=8.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=1.dp))
                Text("ПРЕСС\n${levels[4]} ур.",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=8.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.align(Alignment.BottomStart).padding(bottom=48.dp,start=1.dp))
                Text("РУКИ И ПЛЕЧИ\n${levels[5]} ур.",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=8.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.align(Alignment.TopStart).padding(top=48.dp,start=1.dp))
            }
        }
    }
}

@Composable
private fun HealthPanel(vm:LifeViewModel){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=12.dp, vertical=4.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        when(vm.healthTab){
            "ТРЕНИРОВКИ" -> {
                TrainingCalendarPanel(vm)
                listOf("Грудь","Спина","Руки и плечи","Пресс","Ноги","Выносливость").forEach{HealthMuscleCard(vm,it)}
                TrainingDayPanel(vm)
            }
            "ПИТАНИЕ" -> NutritionWorkspace(vm)
            "СОН" -> SleepWorkspace(vm)
            else -> Unit
        }
    }
}

@Composable
private fun HealthSubTabs(vm:LifeViewModel){
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(1.dp,Grid),shape=RoundedCornerShape(10.dp)){
        Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("ТРЕНИРОВКИ","ПИТАНИЕ","СОН").forEach{t->
                Surface(Modifier.weight(1f).height(32.dp).clickable{vm.healthTab=t},color=if(vm.healthTab==t)Green.copy(alpha=.16f) else Color.Transparent,border=BorderStroke(1.dp,if(vm.healthTab==t)Green else Grid),shape=RoundedCornerShape(7.dp)){
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(t,color=if(vm.healthTab==t)Green else White,fontFamily=blenderBold(),fontSize=10.sp)}
                }
            }
        }
    }
}

private fun ruWeekday(date:String):String = when(LocalDate.parse(date).dayOfWeek.ordinal){
    0->"ПН";1->"ВТ";2->"СР";3->"ЧТ";4->"ПТ";5->"СБ";else->"ВС"
}

@Composable
private fun TrainingCalendarPanel(vm:LifeViewModel){
    var planMenuOpen by remember(vm.selectedTrainingDate) { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(1.dp,Grid),shape=RoundedCornerShape(10.dp)){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("КАЛЕНДАРЬ ТРЕНИРОВОК",color=Green,fontFamily=blenderBold(),fontSize=18.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick={vm.selectedTrainingDate=todayIso()},contentPadding=PaddingValues(horizontal=6.dp,vertical=0.dp)){Text("СЕГОДНЯ",color=Light,fontFamily=blenderBold(),fontSize=9.sp)}
            }
            TrainingCalendarGrid(vm)
            Text("${toDisplayDate(vm.selectedTrainingDate)} • ${ruWeekday(vm.selectedTrainingDate)}",color=White,fontFamily=blenderBook(),fontSize=11.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("По плану: ${vm.trainingPlanLabelFor(vm.selectedTrainingDate)}",color=Light,fontFamily=blenderBold(),fontSize=13.sp)
                    Text("Изменить тренировку на выбранный день", color=Color.Gray, fontFamily=blenderBook(), fontSize=9.sp)
                }
                Box {
                    TextButton(onClick={ planMenuOpen = true }, contentPadding=PaddingValues(horizontal=8.dp, vertical=2.dp)) {
                        Text("ИЗМЕНИТЬ", color=Green, fontFamily=blenderBold(), fontSize=10.sp)
                    }
                    DropdownMenu(expanded=planMenuOpen, onDismissRequest={planMenuOpen=false}, modifier=Modifier.background(Color(0xFF050B10)).border(1.dp,Green,RoundedCornerShape(8.dp))) {
                        listOf("Отдых","Грудь","Спина","Руки и плечи","Пресс","Ноги","Выносливость").forEach { option ->
                            DropdownMenuItem(
                                text={Text(option, color=if(option==vm.trainingGroupFor(vm.selectedTrainingDate) || (option=="Отдых" && vm.trainingGroupFor(vm.selectedTrainingDate).isBlank())) Green else White)},
                                onClick={vm.setTrainingGroupForDate(vm.selectedTrainingDate, if(option=="Отдых") "" else option); planMenuOpen=false}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingCalendarGrid(vm:LifeViewModel){
    val date=LocalDate.parse(vm.selectedTrainingDate); val year=date.year; val month=date.month.ordinal+1
    val first=LocalDate(year,month,1); val days=daysInMonth(year,month); val offset=first.dayOfWeek.ordinal
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){listOf("ПН","ВТ","СР","ЧТ","ПТ","СБ","ВС").forEach{d->Text(d,color=Color.Gray,fontFamily=blenderBold(),fontSize=8.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        TextButton(onClick={vm.selectedTrainingDate=shiftMonth(date,-1).toString()},contentPadding=PaddingValues(0.dp)){Text("‹",fontSize=22.sp,color=Green)}
        Spacer(Modifier.weight(1f));Text("${month}/${year}",color=White,fontFamily=blenderBold(),fontSize=14.sp);Spacer(Modifier.weight(1f))
        TextButton(onClick={vm.selectedTrainingDate=shiftMonth(date,1).toString()},contentPadding=PaddingValues(0.dp)){Text("›",fontSize=22.sp,color=Green)}
    }
    Column{
        for(week in 0..5){
            Row(Modifier.fillMaxWidth()){
                for(col in 0..6){
                    val day=week*7+col-offset+1
                    if(day in 1..days){
                        val iso=LocalDate(year,month,day).toString(); val selected=iso==vm.selectedTrainingDate; val plan=vm.trainingPlanLabelFor(iso)
                        val futureLocked = !vm.isAdmin && iso > vm.appTodayIso()
                        Box(Modifier.weight(1f).height(36.dp).padding(2.dp).clip(RoundedCornerShape(5.dp)).background(if(selected)Color(0xFF79C8EE).copy(alpha=.22f) else Color.Transparent).clickable(enabled=!futureLocked){vm.selectedTrainingDate=iso},contentAlignment=Alignment.Center){
                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                Text(day.toString(),color=if(futureLocked)Color(0xFF4B555C) else if(selected)Color(0xFFB9EAFF) else White,fontFamily=blenderBold(),fontSize=10.sp)
                                Text(when(plan){"Отдых"->"";"Ноги (бег)"->"БЕГ";else->plan.take(1)},color=if(plan=="Отдых")Color.Gray else Light,fontFamily=blenderBold(),fontSize=7.sp)
                                if(plan!="Отдых"){
                                    val g=vm.trainingGroupFor(iso); val pr=vm.trainingDayProgress(g,iso)
                                    Text(if(pr>=1f)"✓" else if(pr>0f)"${vm.trainingDayPercent(g,iso)}%" else "",color=if(pr>=1f)Green else Color(0xFF79C8EE),fontFamily=blenderBold(),fontSize=6.sp)
                                }
                            }
                        }
                    }else Spacer(Modifier.weight(1f).height(36.dp))
                }
            }
        }
    }
}

@Composable
private fun HealthMuscleCard(vm:LifeViewModel,group:String){
    val revision=vm.trainingUiRevision
    val selectedDate=vm.selectedTrainingDate
    BoxWithConstraints(Modifier.fillMaxWidth()){
        // Responsive breakpoints are based on the actual available width, so
        // the same UI works on narrow phones, large phones and tablets.
        val compact=maxWidth < 360.dp
        val veryCompact=maxWidth < 320.dp
        val horizontalPadding=when{ veryCompact -> 9.dp; compact -> 10.dp; else -> 12.dp }
        val verticalPadding=when{ veryCompact -> 8.dp; compact -> 9.dp; else -> 10.dp }
        val titleSize=when{ veryCompact -> 17.sp; compact -> 19.sp; else -> 21.sp }
        val levelSize=when{ veryCompact -> 9.sp; compact -> 10.sp; else -> 11.sp }
        val detailSize=when{ veryCompact -> 7.sp; compact -> 8.sp; else -> 9.sp }
        val daySize=when{ veryCompact -> 8.sp; compact -> 9.sp; else -> 10.sp }
        val segmentHeight=when{ veryCompact -> 8.dp; compact -> 9.dp; else -> 10.dp }
        val segmentGap=when{ veryCompact -> 3.dp; compact -> 4.dp; else -> 5.dp }
        val buttonSize=when{ veryCompact -> 27.dp; else -> 30.dp }
        val level=vm.muscleLevel(group)

        Surface(Modifier.fillMaxWidth(),color=Color(0xFF050B10),border=BorderStroke(1.dp,Color(0xFF134E2C)),shape=RoundedCornerShape(10.dp)){
            Column(Modifier.padding(horizontal=horizontalPadding,vertical=verticalPadding),verticalArrangement=Arrangement.spacedBy(if(veryCompact) 5.dp else 6.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f).padding(end=4.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text(group,color=Green,fontFamily=blenderBold(),fontSize=titleSize,maxLines=1,softWrap=false)
                            Spacer(Modifier.width(if(veryCompact) 5.dp else 7.dp))
                            Text("$level уровень",color=Color(0xFF5AA8FF),fontFamily=blenderBook(),fontSize=levelSize,maxLines=1,softWrap=false)
                        }
                        Text("Сложность: ${vm.trainingDifficultyForLevel(level)} • Неделя: ${vm.trainingProgressWeek(group)}",color=Color(0xFF9FE7FF),fontFamily=blenderBook(),fontSize=detailSize,maxLines=1,softWrap=false)
                    }
                    if(vm.isAdmin){
                        Row(horizontalArrangement=Arrangement.spacedBy(1.dp)){
                            TextButton(onClick={vm.adjustTrainingSegments(group,-1)},modifier=Modifier.size(buttonSize),contentPadding=PaddingValues(0.dp)){Text("−",color=Color(0xFFFF7A7A),fontFamily=blenderBold(),fontSize=if(veryCompact) 15.sp else 17.sp)}
                            TextButton(onClick={vm.adjustTrainingSegments(group,1)},modifier=Modifier.size(buttonSize),contentPadding=PaddingValues(0.dp)){Text("+",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=if(veryCompact) 15.sp else 17.sp)}
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(segmentGap)){
                    // Cumulative exercise progress fills cells across weeks.
                    // Cell count increases by one every five levels.
                    for(i in 0 until vm.muscleSegmentCount(group)){
                        val target=vm.muscleSegmentProgress(group,i).coerceIn(0f,1f)
                        val segment by animateFloatAsState(
                            targetValue=target,
                            animationSpec=tween(650),
                            label="training-segment-$group-$i"
                        )
                        Box(Modifier.weight(1f).height(segmentHeight).clip(RoundedCornerShape(4.dp)).background(Color(0xFF102436))){
                            if(segment>0f){
                                Box(Modifier.fillMaxHeight().fillMaxWidth(segment).background(Color(0xFF71D7FF)))
                            }
                        }
                    }
                }
                Text("Сегодня: ${vm.trainingDayPercent(group,selectedDate)}%",color=Color(0xFFB8DFF0),fontFamily=blenderBook(),fontSize=daySize,maxLines=2,softWrap=true)
            }
        }
    }
}
@Composable
private fun TrainingDayPanel(vm:LifeViewModel){
    val date=vm.selectedTrainingDate; val group=vm.trainingGroupFor(date); val plan=remember(vm.trainingUiRevision,date,group){if(group.isBlank()) emptyList() else vm.trainingPlan(group,date)}
    val locked=!vm.canExecuteTraining(date)
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(2.dp,Color(0xFF135D7A)),shape=RoundedCornerShape(10.dp)){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("ТРЕНИРОВКА",color=Green,fontFamily=blenderBold(),fontSize=20.sp)
                    Text("${toDisplayDate(date)} • ${ruWeekday(date)} • ${vm.trainingPlanLabelFor(date)} • Неделя ${vm.trainingProgressWeek(group)}",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=12.sp)
                }
                if(locked) Text(if(date>vm.appTodayIso())"БУДУЩАЯ" else "ПРОШЕДШАЯ",color=Color(0xFFFFA0A0),fontFamily=blenderBold(),fontSize=9.sp)
            }
            if(group.isBlank()) {
                Text("Восстановление",color=Color.Gray,fontFamily=blenderBook(),fontSize=12.sp)
            } else {
                plan.forEach { exercise ->
                    val done=vm.isTrainingExerciseDone(exercise)
                    val exerciseLocked=locked && !vm.isAdmin
                    val chosen=vm.selectedExerciseKey==exercise.key
                    Surface(Modifier.fillMaxWidth().clickable{vm.selectedExerciseKey=if(chosen)"" else exercise.key},color=if(chosen)Color(0xFF0E2A33) else if(done)Color(0xFF082514) else Color(0xFF071007),border=BorderStroke(if(chosen)2.dp else 1.dp,if(chosen)Color(0xFF71D7FF) else if(done)Green else Grid),shape=RoundedCornerShape(8.dp)){
                        Row(Modifier.padding(horizontal=8.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
                            Checkbox(
                                checked=done,
                                onCheckedChange={if(!exerciseLocked)vm.toggleTrainingExercise(exercise)},
                                enabled=!exerciseLocked,
                                colors=CheckboxDefaults.colors(checkedColor=Green,uncheckedColor=Color.Gray,disabledCheckedColor=Green.copy(alpha=.45f),disabledUncheckedColor=Color.Gray.copy(alpha=.45f))
                            )
                            Column(Modifier.weight(1f)){
                                Text(exercise.name,color=if(done)Color.Gray else White,fontFamily=blenderBold(),fontSize=12.sp,textDecoration=if(done)TextDecoration.LineThrough else null)
                                Text(exercise.prescription,color=Color(0xFF79C8EE),fontFamily=blenderBook(),fontSize=10.sp)
                                Text("Сложность: ${exercise.difficulty}",color=Color(0xFF9FE7FF),fontFamily=blenderBook(),fontSize=9.sp)
                                Text("Как выполнять: ${exercise.description}",color=Color(0xFFB6D5E8),fontFamily=blenderBook(),fontSize=8.sp,maxLines=3)
                            }
                        }
                    }
                }
                if(locked) Text("Отметить упражнения можно только в день по плану.",color=Color(0xFFFFA0A0),fontFamily=blenderBook(),fontSize=9.sp)
                if(plan.isNotEmpty()) Text("Нажмите на упражнение, чтобы выбрать его для удаления.",color=Color.Gray,fontFamily=blenderBook(),fontSize=8.sp)
                if(plan.isNotEmpty()) Text("Выполнено: ${(vm.trainingDayProgress(group,date)*100).toInt()}%",color=Light,fontFamily=blenderBold(),fontSize=12.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                    val chosenEx=plan.firstOrNull{it.key==vm.selectedExerciseKey}
                    TextButton(
                        onClick={chosenEx?.let{ex->vm.dialog=DialogState("Удалить упражнение","Удалить «${ex.name}» из тренировки на ${toDisplayDate(date)}?",{vm.deleteTrainingExercise(ex)})}},
                        enabled=chosenEx!=null && (!locked||vm.isAdmin),
                        modifier=Modifier.height(28.dp),
                        contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)
                    ){Text("УДАЛИТЬ",color=if(chosenEx!=null&&(!locked||vm.isAdmin))Red else Color.Gray,fontFamily=blenderBold(),fontSize=9.sp)}
                    TextButton(
                        onClick={pickTextFile{text->val n=vm.importTrainingText(group,date,text);vm.dialog=DialogState("Загрузка программы",if(n>0)"Добавлено упражнений: $n" else "В файле не найдено упражнений. Формат строки: Поднятие гантелей в стороны 15x3")}},
                        enabled=!locked || vm.isAdmin,
                        modifier=Modifier.height(28.dp),
                        contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)
                    ){Text("ЗАГРУЗИТЬ",color=if(!locked||vm.isAdmin)Light else Color.Gray,fontFamily=blenderBold(),fontSize=9.sp)}
                    TextButton(
                        onClick={vm.openTrainingCreator(group,date)},
                        enabled=!locked || vm.isAdmin,
                        modifier=Modifier.height(28.dp),
                        contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)
                    ){Text("+ СОЗДАТЬ",color=if(!locked||vm.isAdmin)Green else Color.Gray,fontFamily=blenderBold(),fontSize=9.sp)}
                }
            }
        }
    }
}

@Composable private fun MobileSectionTitle(title:String){
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(4.dp).height(24.dp).background(Green))
        Spacer(Modifier.width(8.dp))
        Text(title,color=Green,fontFamily=blenderBold(),fontSize=22.sp)
    }
}

@Composable private fun MobilePlaceholder(title:String){
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(1.dp,Grid),shape=RoundedCornerShape(10.dp)){
        Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text(title,color=Green,fontFamily=blenderBold(),fontSize=24.sp)
            Spacer(Modifier.height(8.dp))
            Text("Раздел готов к работе",color=Color.Gray,fontFamily=blenderBook(),fontSize=13.sp)
        }
    }
}

@Composable
private fun MobileTaskPanel(vm:LifeViewModel){
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(1.dp,Grid),shape=RoundedCornerShape(10.dp)){
        Column(Modifier.padding(12.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.width(4.dp).height(22.dp).background(Green))
                Spacer(Modifier.width(7.dp))
                Text("ЗАДАЧИ",color=Green,fontFamily=blenderBold(),fontSize=20.sp)
                Spacer(Modifier.weight(1f))
                CreateTaskButton{vm.openCreate()}
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                FilterButton("АКТИВНЫЕ",vm.taskView=="ACTIVE"){vm.taskView="ACTIVE"}
                FilterButton("ЗАВЕРШЕННЫЕ",vm.taskView=="DONE"){vm.taskView="DONE"}
            }
            Spacer(Modifier.height(6.dp))
            val list=vm.tasks.filter{it.date==vm.selectedTaskDate&&it.done==(vm.taskView=="DONE")}
            if(list.isEmpty()) Text("Нет задач",color=Color.Gray,fontFamily=blenderBook(),modifier=Modifier.padding(8.dp))
            list.forEach{MobileTaskRow(vm,it)}
        }
    }
}

@Composable
private fun MobileTaskRow(vm:LifeViewModel,t:TaskItem){
    val focused = vm.focusedTask === t
    Surface(Modifier.fillMaxWidth().padding(vertical=4.dp),color=if(focused)Color(0xFF0A2616) else Color(0xFF071007),border=BorderStroke(if(focused)2.dp else 1.dp,if(focused)Green else Grid),shape=RoundedCornerShape(8.dp)){
        Column(Modifier.padding(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Checkbox(t.done,{if(it)vm.complete(t)else vm.restore(t)},colors=CheckboxDefaults.colors(checkedColor=Green))
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)){
                    Text(t.text,color=if(t.done)Color.Gray else White,fontFamily=blenderBook(),fontSize=14.sp,maxLines=3,textDecoration=if(t.done)TextDecoration.LineThrough else null)
                    if(t.description.isNotBlank()) Text(t.description,color=Color(0xFFB0B0B0),fontFamily=blenderBook(),fontSize=11.sp,maxLines=4,modifier=Modifier.padding(top=3.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("${t.start_time} • ${t.duration_minutes} мин",color=Color.Gray,fontFamily=blenderBook(),fontSize=10.sp)
                    DifficultyChip(t.difficulty)
                }
                Spacer(Modifier.width(8.dp))
                Text("+${t.points} XP",color=Light,fontFamily=blenderBold(),fontSize=20.sp,maxLines=1)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                TextButton(onClick={vm.openEdit(t)}){Text("ИЗМЕНИТЬ",color=Light,fontFamily=blenderBold(),fontSize=10.sp)}
                TextButton(onClick={vm.delete(t)}){Text("УДАЛИТЬ",color=Red,fontFamily=blenderBold(),fontSize=10.sp)}
            }
        }
    }
}

@Composable
private fun DifficultyChip(difficulty:String){
    val c=when(difficulty){
        "Легкий"->Color(0xFF45E06B)
        "Средний"->Color(0xFFFFD35A)
        "Тяжелый"->Color(0xFFFF9A3D)
        "Экстремальный"->Color(0xFFFF5252)
        else->White
    }
    Surface(color=c.copy(alpha=.14f),border=BorderStroke(1.dp,c),shape=RoundedCornerShape(6.dp),modifier=Modifier.padding(top=4.dp)){
        Text(difficulty,color=c,fontFamily=blenderBold(),fontSize=10.sp,modifier=Modifier.padding(horizontal=7.dp,vertical=3.dp))
    }
}

@Composable
private fun MobileCalendarPanel(vm:LifeViewModel){
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(1.dp,Grid),shape=RoundedCornerShape(10.dp)){
        Column(Modifier.padding(12.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("КАЛЕНДАРЬ",color=Green,fontFamily=blenderBold(),fontSize=20.sp)
                Spacer(Modifier.weight(1f))
                FilterButton("ОБЫЧНЫЙ",!vm.calendarR){vm.calendarR=false}
                FilterButton("РАБОЧИЙ",vm.calendarR){vm.calendarR=true}
            }
            Spacer(Modifier.height(4.dp))
            CalendarGrid(vm)
        }
    }
}

@Composable
private fun MobileWorkload(vm:LifeViewModel){
    Surface(Modifier.fillMaxWidth(),color=Panel,border=BorderStroke(1.dp,Grid),shape=RoundedCornerShape(10.dp)){
        Column(Modifier.padding(14.dp)){
            Text("ЗАГРУЖЕННОСТЬ",color=Green,fontFamily=blenderBold(),fontSize=18.sp)
            val mins=vm.tasks.filter{it.date==vm.selectedTaskDate}.sumOf{it.duration_minutes}
            Text("${mins/60} ч ${mins%60} мин",color=White,fontFamily=blenderBold(),fontSize=30.sp,modifier=Modifier.padding(vertical=8.dp))
            Text(if(mins>960)"Время сокращено на ${(mins-960)/60} ч" else "Норма сна: 8 часов",
                color=if(mins>960)Red else Light,fontFamily=blenderBook(),fontSize=12.sp)
            Spacer(Modifier.height(5.dp))
            Text("Лимит работы: 16 часов",color=Color.Gray,fontFamily=blenderBook(),fontSize=11.sp)
            Spacer(Modifier.height(4.dp))
            ProgressBar((mins/960f).coerceIn(0f,1f),Color(0xFF0A3A1A),Green)
        }
    }
}

@Composable
private fun NotesWorkspace(vm: LifeViewModel) {
    val notes = vm.data.notes
    val selected = notes.firstOrNull { it.id == vm.selectedNoteId } ?: notes.firstOrNull()
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("ЗАМЕТКИ", color = Green, fontFamily = blenderBold(), fontSize = 22.sp)
                    Spacer(Modifier.weight(1f))
                    GreenButton("+ НОВАЯ") { vm.createNote() }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    notes.forEach { note ->
                        Surface(
                            Modifier.clickable { vm.selectedNoteId = note.id },
                            color = if (selected?.id == note.id) Color(0xFF1A241D) else Color(0xFF111519),
                            border = BorderStroke(1.dp, if (selected?.id == note.id) Green else Color(0xFF353B3F)),
                            shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(note.title.ifBlank { "Без названия" }, color = if (selected?.id == note.id) White else Color.LightGray, fontFamily = blenderBook(), fontSize = 12.sp, maxLines = 1)
                                Spacer(Modifier.width(5.dp))
                                Text("×", color = Red, fontSize = 12.sp, modifier = Modifier.clickable { vm.removeNote(note.id) })
                            }
                        }
                    }
                    if (notes.isEmpty()) Text("Создайте первую заметку", color = Color.Gray, fontFamily = blenderBook(), fontSize = 12.sp)
                }
            }
        }
        if (selected == null) {
            Surface(Modifier.fillMaxWidth().weight(1f), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ваше пространство заметок", color = Green, fontFamily = blenderBold(), fontSize = 22.sp)
                    Text("Создавайте заметки, задавайте тему и соединяйте материалы между собой.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 13.sp)
                    GreenButton("СОЗДАТЬ ЗАМЕТКУ") { vm.createNote() }
                }
            }
        } else {
            NotesEditorPane(vm, selected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NotesEditorPane(vm: LifeViewModel, note: NoteItem, modifier: Modifier = Modifier) {
    var linkMenu by remember(note.id) { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("НАЗВАНИЕ ЗАМЕТКИ", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("${note.title.length}/30", color = if (note.title.length >= 30) Red else Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
            }
            OutlinedTextField(
                value = note.title,
                onValueChange = { vm.updateNote(note.copy(title = it.take(30))) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = White, fontSize = 21.sp, fontFamily = blenderBold()),
                colors = darkFields(), placeholder = { Text("Название заметки (до 30 символов)", color = Color.Gray) }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ТЕМА ЗАМЕТКИ", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("${note.topic.length}/45", color = if (note.topic.length >= 45) Red else Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
            }
            OutlinedTextField(
                value = note.topic,
                onValueChange = { vm.updateNote(note.copy(topic = it.take(45))) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = Color(0xFFB0D9B0), fontSize = 14.sp, fontFamily = blenderBook()),
                colors = darkFields(), placeholder = { Text("Например: Изучение JavaScript (до 45 символов)", color = Color.Gray) }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("СОДЕРЖАНИЕ", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("Сохранение автоматическое", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
            }
            OutlinedTextField(
                value = note.content,
                onValueChange = { vm.updateNote(note.copy(content = it)) },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 270.dp),
                minLines = 11, maxLines = 24,
                textStyle = TextStyle(color = White, fontSize = 13.sp, fontFamily = blenderBook(), lineHeight = 19.sp),
                colors = darkFields(), placeholder = { Text("Пишите текст, добавляйте списки и ссылки…", color = Color.Gray) }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("СВЯЗИ", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                    Text(if (note.linked_note_ids.isEmpty()) "Пока нет связанных заметок" else "Связано: ${note.linked_note_ids.size}", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                }
                Box {
                    GreenButton("СВЯЗАТЬ ЗАМЕТКУ") { linkMenu = true }
                    DropdownMenu(expanded = linkMenu, onDismissRequest = { linkMenu = false }) {
                        vm.data.notes.filter { it.id != note.id }.forEach { other ->
                            val linked = note.linked_note_ids.contains(other.id)
                            DropdownMenuItem(
                                text = { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = linked, onCheckedChange = null); Spacer(Modifier.width(5.dp)); Text(other.title.ifBlank { "Без названия" }, maxLines = 1) } },
                                onClick = {
                                    val links = note.linked_note_ids.toMutableList()
                                    if (linked) links.remove(other.id) else links.add(other.id)
                                    vm.updateNote(note.copy(linked_note_ids = links))
                                }
                            )
                        }
                        if (vm.data.notes.size <= 1) DropdownMenuItem(text = { Text("Добавьте ещё одну заметку") }, onClick = { linkMenu = false }, enabled = false)
                    }
                }
            }
            if (note.linked_note_ids.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    vm.data.notes.filter { note.linked_note_ids.contains(it.id) }.forEach { linked ->
                        Surface(color = Color(0xFF14271A), border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(6.dp)) {
                            Text(linked.title, color = Light, fontFamily = blenderBook(), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesListWorkspace(vm: LifeViewModel) {
    val notes = vm.data.notes
    var expandedId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("СПИСОК ЗАМЕТОК", color = Green, fontFamily = blenderBold(), fontSize = 23.sp)
                Text("Нажмите на карточку, чтобы раскрыть всю информацию заметки", color = Color.Gray, fontFamily = blenderBook(), fontSize = 11.sp)
            }
            GreenButton("+ ЗАМЕТКА") { vm.createNote() }
        }
        if (notes.isEmpty()) {
            EmptyFeatureCard("Список заметок пуст", "Создайте первую заметку — она появится здесь в виде карточки.") { vm.createNote() }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = maxOf(1, (maxWidth / 250.dp).toInt())
                notes.chunked(columns).forEach { rowNotes ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowNotes.forEach { note ->
                            NoteListCard(
                                vm = vm,
                                note = note,
                                expanded = expandedId == note.id,
                                onToggle = { expandedId = if (expandedId == note.id) null else note.id },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowNotes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Text("Заметок: ${notes.size}", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
        }
    }
}

@Composable
private fun NoteListCard(vm: LifeViewModel, note: NoteItem, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.clickable { onToggle() },
        color = if (expanded) Color(0xFF14261A) else Color(0xF20B1410),
        border = BorderStroke(if (expanded) 1.5.dp else 1.dp, if (expanded) Green else Grid),
        shape = RoundedCornerShape(9.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(note.title.ifBlank { "Без названия" }, color = White, fontFamily = blenderBold(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(5.dp))
                Text("×", color = Red, fontSize = 12.sp, modifier = Modifier.clickable { vm.removeNote(note.id) })
            }
            Text(note.topic.ifBlank { "Без темы" }, color = Light, fontFamily = blenderBook(), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (expanded) {
                HorizontalDivider(color = Grid, thickness = 1.dp)
                if (note.content.isBlank()) {
                    Text("Текст заметки пуст", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                } else {
                    Text(note.content, color = White, fontFamily = blenderBook(), fontSize = 11.sp, lineHeight = 16.sp)
                }
                if (note.linked_note_ids.isNotEmpty()) {
                    Text("СВЯЗАННЫЕ ЗАМЕТКИ", color = Light, fontFamily = blenderBold(), fontSize = 9.sp)
                    vm.data.notes.filter { note.linked_note_ids.contains(it.id) }.forEach { linked ->
                        Text("• ${linked.title.ifBlank { "Без названия" }}", color = Light, fontFamily = blenderBook(), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text("Обновлено: ${note.updated_at}", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
                GreenButton("РЕДАКТИРОВАТЬ") { vm.selectedNoteId = note.id; vm.activeTab = "ЗАМЕТКИ" }
            } else {
                Text("${note.updated_at} • связей: ${note.linked_note_ids.size}", color = Color.Gray, fontFamily = blenderBook(), fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DetailedPlansWorkspace(vm: LifeViewModel) {
    val plans = vm.data.detailed_plans
    val selected = plans.firstOrNull { it.id == vm.selectedPlanId } ?: plans.firstOrNull()
    var planListMenu by remember { mutableStateOf(false) }
    var planQuery by remember { mutableStateOf("") }
    var createOpen by remember { mutableStateOf(false) }
    if (createOpen) PlanCreateDialog(onDismiss = { createOpen = false }, onCreate = { t, c -> createOpen = false; vm.createDetailedPlan(t, c) })
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ПЛАНЫ В ПОДРОБНОСТЯХ", color = Green, fontFamily = blenderBold(), fontSize = 22.sp)
                Text("Большие описания, подпункты, форматирование и связи между планами", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
            }
            GreenButton("+ СОЗДАТЬ ПЛАН") { createOpen = true }
        }
        if (plans.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ТЕКУЩИЙ ПЛАН", color = Light, fontFamily = blenderBold(), fontSize = 10.sp)
                    Text(selected?.title?.ifBlank { "Без названия" } ?: "—", color = White, fontFamily = blenderBold(), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    GreenButton("СПИСОК ПЛАНОВ ▾") { planListMenu = true }
                    DropdownMenu(expanded = planListMenu, onDismissRequest = { planListMenu = false }) {
                        Column(Modifier.width(320.dp)) {
                            OutlinedTextField(
                                value = planQuery,
                                onValueChange = { planQuery = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                singleLine = true,
                                textStyle = TextStyle(color = White, fontSize = 12.sp, fontFamily = blenderBook()),
                                colors = darkFields(),
                                placeholder = { Text("Поиск плана…", color = Color.Gray, fontSize = 12.sp) },
                                trailingIcon = { if (planQuery.isNotEmpty()) Text("×", color = Red, fontSize = 16.sp, modifier = Modifier.clickable { planQuery = "" }.padding(horizontal = 4.dp)) }
                            )
                            Text("ПЛАНОВ: ${plans.size}", color = Color.Gray, fontFamily = blenderBold(), fontSize = 8.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            val query = planQuery.trim()
                            val filtered = if (query.isEmpty()) plans else plans.filter { it.title.contains(query, ignoreCase = true) }
                            Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                                if (filtered.isEmpty()) {
                                    Text("Ничего не найдено", color = Color.Gray, fontFamily = blenderBook(), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                                }
                                filtered.forEach { plan ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    plan.title.ifBlank { "Без названия" },
                                                    color = if (plan.id == selected?.id) Light else White,
                                                    fontFamily = blenderBook(),
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("подп.: ${plan.subitems.size} • связ.: ${plan.linked_plan_ids.size}", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
                                            }
                                        },
                                        leadingIcon = { if (plan.id == selected?.id) Text("●", color = Green, fontSize = 10.sp) else Text("○", color = Grid, fontSize = 10.sp) },
                                        onClick = { vm.selectedPlanId = plan.id; planListMenu = false; planQuery = "" }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (selected == null) {
            EmptyFeatureCard("Подробные планы", "Создайте план, добавьте длинное описание и разбейте его на подпункты.") { createOpen = true }
        } else {
            PlanEditorPane(vm, selected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlanEditorPane(vm: LifeViewModel, plan: DetailedPlanItem, modifier: Modifier = Modifier) {
    var linkMenu by remember(plan.id) { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("НАЗВАНИЕ ПЛАНА", color = Light, fontFamily = blenderBold(), fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { vm.removeDetailedPlan(plan.id) }) { Text("УДАЛИТЬ", color = Red, fontFamily = blenderBold(), fontSize = 10.sp) }
            }
            OutlinedTextField(value = plan.title, onValueChange = { vm.updateDetailedPlan(plan.copy(title = it)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = White, fontSize = 20.sp, fontFamily = blenderBold()), colors = darkFields(), placeholder = { Text("Название плана", color = Color.Gray) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ПОДРОБНОЕ ОПИСАНИЕ", color = Green, fontFamily = blenderBold(), fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                GreenButton("+ ПОДПУНКТ") { vm.addPlanSubItem(plan.id) }
            }
            OutlinedTextField(value = plan.description, onValueChange = { vm.updateDetailedPlan(plan.copy(description = it)) }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp), minLines = 5, maxLines = 15, textStyle = TextStyle(color = White, fontSize = 12.sp, fontFamily = blenderBook(), lineHeight = 18.sp), colors = darkFields(), placeholder = { Text("Опишите этапы, критерии и необходимые материалы…", color = Color.Gray) })
            if (plan.subitems.isEmpty()) Text("Добавьте подпункты прямо в подробное описание: их можно выделять, нумеровать, зачеркивать и окрашивать.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
            plan.subitems.forEachIndexed { index, item ->
                Surface(color = Color(0xFF090E0B), border = BorderStroke(1.dp, Color(0xFF243C29)), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.fillMaxWidth().padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (item.numbered) Text("${index + 1}.", color = Light, fontFamily = blenderBold(), fontSize = 12.sp)
                            OutlinedTextField(
                                value = item.text,
                                onValueChange = { vm.updatePlanSubItem(plan.id, item.copy(text = it)) },
                                modifier = Modifier.weight(1f), singleLine = false, minLines = 1, maxLines = 4,
                                textStyle = TextStyle(color = White, fontSize = 12.sp, fontFamily = blenderBook(), textDecoration = if (item.struck) TextDecoration.LineThrough else TextDecoration.None, background = if (item.highlighted) parseUiColor(item.highlight_color).copy(alpha = 0.22f) else Color.Transparent),
                                colors = darkFields(), placeholder = { Text("Текст подпункта", color = Color.Gray) }
                            )
                            Text("×", color = Red, fontSize = 17.sp, modifier = Modifier.clickable { vm.updateDetailedPlan(plan.copy(subitems = plan.subitems.filterNot { it.id == item.id }.toMutableList())) }.padding(horizontal = 5.dp))
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            MiniToggle("№ Номер", item.numbered) { vm.updatePlanSubItem(plan.id, item.copy(numbered = !item.numbered)) }
                            MiniToggle("S̶ Зачеркнуть", item.struck) { vm.updatePlanSubItem(plan.id, item.copy(struck = !item.struck)) }
                            MiniToggle("Выделить", item.highlighted) { vm.updatePlanSubItem(plan.id, item.copy(highlighted = !item.highlighted)) }
                            listOf("#243A25", "#66501B", "#203B67", "#52264E").forEach { hex ->
                                Box(Modifier.size(21.dp).clip(CircleShape).background(parseUiColor(hex)).border(1.dp, if (item.highlight_color == hex) White else Color.Gray, CircleShape).clickable { vm.updatePlanSubItem(plan.id, item.copy(highlight_color = hex, highlighted = true)) })
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clickable { vm.setCentralPlan(plan.id, !plan.is_central) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = plan.is_central, onCheckedChange = { vm.setCentralPlan(plan.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Green))
                Text("Центральный план на карте планов", color = Light, fontFamily = blenderBook(), fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("СВЯЗАННЫЕ ПЛАНЫ", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                    Text(if (plan.linked_plan_ids.isEmpty()) "Свяжите план с другими планами" else "Связано: ${plan.linked_plan_ids.size}", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                }
                Box {
                    GreenButton("СВЯЗАТЬ ПЛАН") { linkMenu = true }
                    DropdownMenu(expanded = linkMenu, onDismissRequest = { linkMenu = false }) {
                        Column(Modifier.width(300.dp).heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                            vm.data.detailed_plans.filter { it.id != plan.id }.forEach { other ->
                                val linked = plan.linked_plan_ids.contains(other.id)
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = linked, onCheckedChange = null); Spacer(Modifier.width(5.dp)); Text(other.title.ifBlank { "Без названия" }, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                                    onClick = {
                                        val links = plan.linked_plan_ids.toMutableList()
                                        if (linked) links.remove(other.id) else links.add(other.id)
                                        vm.updateDetailedPlan(plan.copy(linked_plan_ids = links))
                                    }
                                )
                            }
                            if (vm.data.detailed_plans.size <= 1) DropdownMenuItem(text = { Text("Добавьте ещё один план") }, onClick = { linkMenu = false }, enabled = false)
                        }
                    }
                }
            }
            if (plan.linked_plan_ids.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    vm.data.detailed_plans.filter { plan.linked_plan_ids.contains(it.id) }.forEach { linked ->
                        Surface(
                            color = Color(0xFF14271A), border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { vm.selectedPlanId = linked.id }
                        ) {
                            Text("→ ${linked.title.ifBlank { "Без названия" }}", color = Light, fontFamily = blenderBook(), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), maxLines = 1)
                        }
                    }
                }
                Text("Нажмите на связанный план, чтобы открыть его", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun MiniToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(Modifier.clickable(onClick = onClick), color = if (selected) Green.copy(alpha = 0.18f) else Color(0xFF111713), border = BorderStroke(1.dp, if (selected) Green else Grid), shape = RoundedCornerShape(5.dp)) {
        Text(label, color = if (selected) Light else Color.LightGray, fontFamily = blenderBook(), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
    }
}

private fun parseUiColor(hex: String): Color = runCatching {
    val value = hex.removePrefix("#").toLong(16)
    if (hex.length <= 7) Color(0xFF000000L or value) else Color(value)
}.getOrElse { Green }

@Composable
private fun NutritionWorkspace(vm: LifeViewModel) {
    var foodName by remember(vm.nutritionDate) { mutableStateOf("") }
    var caloriesText by remember(vm.nutritionDate) { mutableStateOf("") }
    var proteinText by remember(vm.nutritionDate) { mutableStateOf("") }
    var fatText by remember(vm.nutritionDate) { mutableStateOf("") }
    var carbsText by remember(vm.nutritionDate) { mutableStateOf("") }
    var meal by remember(vm.nutritionDate) { mutableStateOf("Завтрак") }
    var targetText by remember(vm.data.nutrition_calorie_target) { mutableStateOf(vm.data.nutrition_calorie_target.toString()) }
    var error by remember { mutableStateOf("") }
    val entries = vm.data.food_entries.filter { it.date == vm.nutritionDate }
    val consumed = entries.sumOf { it.calories }
    val target = vm.data.nutrition_calorie_target.coerceAtLeast(1)
    val protein = entries.sumOf { it.protein_grams }
    val fat = entries.sumOf { it.fat_grams }
    val carbs = entries.sumOf { it.carbs_grams }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ПИТАНИЕ", color = Green, fontFamily = blenderBold(), fontSize = 22.sp)
                Text("Дневник питания", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = targetText, onValueChange = { targetText = it.filter(Char::isDigit) }, modifier = Modifier.width(100.dp), singleLine = true, label = { Text("Цель, ккал") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                TextButton(onClick = { targetText.toIntOrNull()?.let { vm.updateNutritionTarget(it); error = "" } ?: run { error = "Цель должна быть числом." } }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("✓", color = Green, fontSize = 16.sp) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { vm.nutritionDate = LocalDate.parse(vm.nutritionDate).minus(DatePeriod(days = 1)).toString() }) { Text("‹", color = Green, fontSize = 22.sp) }
            Text(vm.nutritionDate, color = White, fontFamily = blenderBold(), fontSize = 12.sp)
            TextButton(onClick = { vm.nutritionDate = LocalDate.parse(vm.nutritionDate).plus(DatePeriod(days = 1)).toString() }) { Text("›", color = Green, fontSize = 22.sp) }
            TextButton(onClick = { vm.nutritionDate = todayIso() }) { Text("СЕГОДНЯ", color = Light, fontFamily = blenderBold(), fontSize = 9.sp) }
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    NutritionMetric("СЪЕДЕНО", "$consumed", "ккал", modifier = Modifier.weight(1f), tint = Color(0xFF75D49A))
                    Box(Modifier.weight(1.2f).height(104.dp).clip(RoundedCornerShape(55.dp)).background(Color(0xFF0A1D11)).border(2.dp, Green.copy(alpha = 0.7f), RoundedCornerShape(55.dp)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$target", color = White, fontFamily = blenderBold(), fontSize = 25.sp)
                            Text("НОРМА", color = Light, fontFamily = blenderBold(), fontSize = 9.sp)
                        }
                    }
                    NutritionMetric("ОСТАЛОСЬ", "${(target - consumed).coerceAtLeast(0)}", "ккал", modifier = Modifier.weight(1f), tint = if (consumed > target) Red else Light)
                }
                Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF17311E))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((consumed.toFloat() / target).coerceIn(0f, 1f)).background(if (consumed > target) Red else Green))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MacroChip("БЕЛКИ", "${protein.toInt()} г", Modifier.weight(1f))
                    MacroChip("ЖИРЫ", "${fat.toInt()} г", Modifier.weight(1f))
                    MacroChip("УГЛЕВОДЫ", "${carbs.toInt()} г", Modifier.weight(1f))
                }
            }
        }
        var mealFilter by remember { mutableStateOf("Завтрак") }
        var editingDish by remember { mutableStateOf<Dish?>(null) }
        LaunchedEffect(Unit) { vm.ensureDishes() }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("ВЫБОР БЛЮДА", color = Green, fontFamily = blenderBold(), fontSize = 16.sp, modifier = Modifier.weight(1f))
                    GreenButton("+ СВОЁ БЛЮДО") { editingDish = Dish(id = "", meal = mealFilter) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Завтрак", "Обед", "Ужин", "Перекус").forEach { option -> MiniToggle(option, mealFilter == option) { mealFilter = option } }
                }
            }
        }
        val dishes = vm.data.dishes.filter { it.meal == mealFilter }
        if (dishes.isEmpty()) Text("В этой категории пока нет блюд. Нажмите «+ СВОЁ БЛЮДО».", color = Color.Gray, fontFamily = blenderBook(), fontSize = 11.sp)
        dishes.forEach { dish ->
            DishCard(vm, dish, entries.count { it.name == dish.name }) { editingDish = dish }
        }
        editingDish?.let { d -> DishEditDialog(d, { editingDish = null }, { vm.saveDish(it); editingDish = null }, { vm.deleteDish(d.id); editingDish = null }) }
    }
}

private fun numText(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
private fun DishCard(vm: LifeViewModel, dish: Dish, eaten: Int, onEdit: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dish.name, color = White, fontFamily = blenderBold(), fontSize = 16.sp)
                    Text(dish.meal.uppercase(), color = Light, fontFamily = blenderBold(), fontSize = 9.sp)
                }
                Text("✎", color = Light, fontSize = 20.sp, modifier = Modifier.clickable { onEdit() }.padding(6.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MacroChip("ККАЛ", "${dish.calories}", Modifier.weight(1f))
                MacroChip("БЕЛКИ", "${numText(dish.protein_grams)} г", Modifier.weight(1f))
                MacroChip("ЖИРЫ", "${numText(dish.fat_grams)} г", Modifier.weight(1f))
                MacroChip("УГЛЕВОДЫ", "${numText(dish.carbs_grams)} г", Modifier.weight(1f))
            }
            Text("РЕЦЕПТ ПРИГОТОВЛЕНИЯ", color = Green, fontFamily = blenderBold(), fontSize = 10.sp)
            Text(dish.recipe.ifBlank { "Рецепт не добавлен." }, color = Color(0xFFD0D8D0), fontFamily = blenderBook(), fontSize = 12.sp, lineHeight = 17.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GreenButton("+ В ДНЕВНИК") { vm.addDishToDiary(dish) }
                Spacer(Modifier.weight(1f))
                if (eaten > 0) {
                    Text("Съедено: ${eaten}×", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                    TextButton(onClick = { vm.removeDishFromDiary(dish) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("−", color = Red, fontSize = 20.sp) }
                }
            }
        }
    }
}

@Composable
private fun DishEditDialog(dish: Dish, onDismiss: () -> Unit, onSave: (Dish) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(dish.name.takeIf { dish.id.isNotBlank() } ?: "") }
    var meal by remember { mutableStateOf(dish.meal) }
    var kcal by remember { mutableStateOf(if (dish.id.isBlank()) "" else dish.calories.toString()) }
    var protein by remember { mutableStateOf(if (dish.id.isBlank()) "" else numText(dish.protein_grams)) }
    var fat by remember { mutableStateOf(if (dish.id.isBlank()) "" else numText(dish.fat_grams)) }
    var carbs by remember { mutableStateOf(if (dish.id.isBlank()) "" else numText(dish.carbs_grams)) }
    var recipe by remember { mutableStateOf(dish.recipe) }
    var error by remember { mutableStateOf("") }
    fun dec(s: String) = s.replace(',', '.').toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF050B10),
        shape = RoundedCornerShape(16.dp),
        title = { Text(if (dish.id.isBlank()) "НОВОЕ БЛЮДО" else "РЕДАКТИРОВАТЬ БЛЮДО", color = Green, fontFamily = blenderBold(), fontSize = 17.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorField("Название блюда", name) { name = it.take(60) }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Завтрак", "Обед", "Ужин", "Перекус").forEach { option -> MiniToggle(option, meal == option) { meal = option } }
                }
                EditorField("Калории, ккал", kcal) { kcal = it.filter(Char::isDigit) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditorField("Белки, г", protein, Modifier.weight(1f)) { protein = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }
                    EditorField("Жиры, г", fat, Modifier.weight(1f)) { fat = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }
                    EditorField("Углеводы, г", carbs, Modifier.weight(1f)) { carbs = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }
                }
                OutlinedTextField(value = recipe, onValueChange = { recipe = it }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp).padding(vertical = 4.dp), minLines = 5, maxLines = 12, label = { Text("Рецепт приготовления") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                if (error.isNotBlank()) Text(error, color = Red, fontFamily = blenderBook(), fontSize = 10.sp)
                if (dish.id.isNotBlank()) TextButton(onClick = onDelete) { Text("УДАЛИТЬ БЛЮДО", color = Red, fontFamily = blenderBold(), fontSize = 11.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) error = "Введите название блюда"
                else onSave(dish.copy(name = name.trim(), meal = meal, calories = kcal.toIntOrNull() ?: 0, protein_grams = dec(protein), fat_grams = dec(fat), carbs_grams = dec(carbs), recipe = recipe))
            }) { Text("Сохранить", color = Green, fontFamily = blenderBold()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Red) } }
    )
}

@Composable
private fun NutritionMetric(label: String, value: String, unit: String, modifier: Modifier = Modifier, tint: Color = Light) {
    Column(modifier.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(label, color = Color.Gray, fontFamily = blenderBold(), fontSize = 8.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(value, color = tint, fontFamily = blenderBold(), fontSize = 19.sp, maxLines = 1)
        Text(unit, color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
    }
}

@Composable
private fun MacroChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color(0xFF0B140E), border = BorderStroke(1.dp, Color(0xFF203B25)), shape = RoundedCornerShape(7.dp)) {
        Column(Modifier.padding(vertical = 7.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.Gray, fontFamily = blenderBold(), fontSize = 8.sp)
            Text(value, color = White, fontFamily = blenderBold(), fontSize = 11.sp)
        }
    }
}

private fun durationMinutes(bedtime: String, wakeTime: String): Int {
    fun parse(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
    val start = parse(bedtime) ?: return 0
    val end = parse(wakeTime) ?: return 0
    return ((end - start + 1440) % 1440).takeIf { it > 0 } ?: 0
}

@Composable
private fun SleepWorkspace(vm: LifeViewModel) {
    val date = vm.sleepDate
    val stored = vm.data.sleep_records.firstOrNull { it.date == date }
    var bedPicker by remember { mutableStateOf(false) }
    var wakePicker by remember { mutableStateOf(false) }
    var bedtime by remember(date, stored?.bedtime) { mutableStateOf(stored?.bedtime ?: "23:00") }
    var wakeTime by remember(date, stored?.wake_time) { mutableStateOf(stored?.wake_time ?: "07:00") }
    var quality by remember(date, stored?.quality) { mutableFloatStateOf((stored?.quality ?: 80).toFloat()) }
    var notes by remember(date, stored?.notes) { mutableStateOf(stored?.notes.orEmpty()) }
    var savedLabel by remember { mutableStateOf("") }
    val minutes = durationMinutes(bedtime, wakeTime)
    val recent = vm.data.sleep_records.sortedBy { it.date }
    val recommended = recommendedSleepMinutes(recent)
    val advice = sleepBedtimeAdvice(recent)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (bedPicker) TimeWheelDialog("ЛЁГ СПАТЬ", parseClockMinutes(bedtime), { bedPicker = false }) { m -> bedtime = fmtClock(m); bedPicker = false }
        if (wakePicker) TimeWheelDialog("ПРОСНУЛСЯ", parseClockMinutes(wakeTime), { wakePicker = false }) { m -> wakeTime = fmtClock(m); wakePicker = false }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("СОН", color = Green, fontFamily = blenderBold(), fontSize = 22.sp)
                Text("Восстановление и режим сна", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
            }
            Surface(color = Color(0xFF111625), border = BorderStroke(1.dp, Color(0xFF5D5AAF)), shape = RoundedCornerShape(9.dp)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(quality.toInt().toString(), color = Color(0xFFBA9BFF), fontFamily = blenderBold(), fontSize = 26.sp)
                    Text(when { quality >= 90 -> "ОТЛИЧНО"; quality >= 75 -> "ХОРОШО"; quality >= 60 -> "СРЕДНЕ"; else -> "НУЖЕН ОТДЫХ" }, color = White, fontFamily = blenderBold(), fontSize = 8.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { vm.sleepDate = LocalDate.parse(vm.sleepDate).minus(DatePeriod(days = 1)).toString() }) { Text("‹", color = Color(0xFFAD8AFF), fontSize = 22.sp) }
            Text(date, color = White, fontFamily = blenderBold(), fontSize = 12.sp)
            TextButton(onClick = { vm.sleepDate = LocalDate.parse(vm.sleepDate).plus(DatePeriod(days = 1)).toString() }) { Text("›", color = Color(0xFFAD8AFF), fontSize = 22.sp) }
            TextButton(onClick = { vm.sleepDate = todayIso() }) { Text("СЕГОДНЯ", color = Color(0xFFAD8AFF), fontFamily = blenderBold(), fontSize = 9.sp) }
        }
        Surface(Modifier.fillMaxWidth(), color = Color(0xFF111625), border = BorderStroke(1.dp, Color(0xFF5D5AAF)), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("РЕКОМЕНДУЕМОЕ ВРЕМЯ СНА", color = Color(0xFFBA9BFF), fontFamily = blenderBold(), fontSize = 10.sp)
                Text(fmtDuration(recommended), color = White, fontFamily = blenderBold(), fontSize = 26.sp)
                if (advice != null) Text("Совет: ложиться до $advice", color = Light, fontFamily = blenderBold(), fontSize = 12.sp)
                Text(if (recent.none { it.quality >= 80 }) "Ориентир — 8 часов. После записей с оценкой от 80% рекомендация подстроится под вас." else "Рассчитано по вашей статистике сна и оценкам качества.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
            }
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("МОЙ РЕЖИМ", color = Green, fontFamily = blenderBold(), fontSize = 16.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(Modifier.weight(1f).clickable { bedPicker = true }, color = Color(0xFF0A110C), border = BorderStroke(1.dp, Green), shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Лёг спать", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                            Text(fmtClock(parseClockMinutes(bedtime)), color = White, fontFamily = blenderBold(), fontSize = 20.sp)
                        }
                    }
                    Surface(Modifier.weight(1f).clickable { wakePicker = true }, color = Color(0xFF0A110C), border = BorderStroke(1.dp, Green), shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Проснулся", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                            Text(fmtClock(parseClockMinutes(wakeTime)), color = White, fontFamily = blenderBold(), fontSize = 20.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Время сна", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                        Text("${minutes / 60} ч ${minutes % 60} мин", color = White, fontFamily = blenderBold(), fontSize = 23.sp)
                    }
                }
                Text("ОЦЕНКА КАЧЕСТВА СНА • ${quality.toInt()}%", color = Light, fontFamily = blenderBold(), fontSize = 10.sp)
                Slider(value = quality, onValueChange = { quality = it }, valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = Green, activeTrackColor = Green))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 2, maxLines = 4, label = { Text("Заметки о сне") }, textStyle = TextStyle(color = White, fontSize = 11.sp), colors = darkFields())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (savedLabel.isNotBlank()) Text(savedLabel, color = Light, fontFamily = blenderBook(), fontSize = 10.sp, modifier = Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                    GreenButton("СОХРАНИТЬ СОН") {
                        if (durationMinutes(bedtime, wakeTime) > 0) {
                            vm.saveSleepRecord(SleepRecord(date = date, bedtime = bedtime, wake_time = wakeTime, quality = quality.toInt(), snoring_minutes = stored?.snoring_minutes ?: 0, notes = notes))
                            savedLabel = "Запись сохранена"
                        } else savedLabel = "Время пробуждения должно отличаться от времени отхода ко сну"
                    }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("КАЧЕСТВО И ВРЕМЯ СНА", color = Green, fontFamily = blenderBold(), fontSize = 15.sp)
                if (recent.isEmpty()) Text("График появится после сохранения первой записи.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                val timeMax = maxOf(12f, kotlin.math.ceil((recent.maxOfOrNull { durationMinutes(it.bedtime, it.wake_time) } ?: 0) / 60f))
                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    val chartLeft = 8.dp.toPx(); val chartRight = size.width - 8.dp.toPx(); val chartTop = 10.dp.toPx(); val chartBottom = size.height - 12.dp.toPx()
                    for (i in 0..4) {
                        val y = chartTop + (chartBottom - chartTop) * i / 4f
                        drawLine(Color(0xFF25213D), androidx.compose.ui.geometry.Offset(chartLeft, y), androidx.compose.ui.geometry.Offset(chartRight, y), strokeWidth = 1.dp.toPx())
                    }
                    if (recent.isNotEmpty()) {
                        // Every record is always drawn: a new point only compresses the chart, nothing leaves it.
                        fun xAt(index: Int): Float = if (recent.size == 1) (chartLeft + chartRight) / 2f else chartLeft + (chartRight - chartLeft) * index / (recent.size - 1).toFloat()
                        val qPoints = recent.mapIndexed { index, r -> androidx.compose.ui.geometry.Offset(xAt(index), chartBottom - (chartBottom - chartTop) * r.quality.coerceIn(0, 100) / 100f) }
                        val tPoints = recent.mapIndexed { index, r -> androidx.compose.ui.geometry.Offset(xAt(index), chartBottom - (chartBottom - chartTop) * (durationMinutes(r.bedtime, r.wake_time) / 60f / timeMax).coerceIn(0f, 1f)) }
                        listOf(qPoints to Color(0xFFAD8AFF), tPoints to Color(0xFF5CE0C6)).forEach { (pts, col) ->
                            val path = Path().apply { pts.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) } }
                            drawPath(path, col, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                            pts.forEach { drawCircle(col, 3.5.dp.toPx(), it) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("● КАЧЕСТВО, % (0–100)", color = Color(0xFFAD8AFF), fontFamily = blenderBook(), fontSize = 9.sp)
                    Text("● ВРЕМЯ, ч (0–${timeMax.toInt()})", color = Color(0xFF5CE0C6), fontFamily = blenderBook(), fontSize = 9.sp)
                }
                if (recent.isNotEmpty()) recent.takeLast(7).forEach { rec ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(rec.date, color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp, modifier = Modifier.width(90.dp))
                        Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF211D35))) { Box(Modifier.fillMaxHeight().fillMaxWidth(rec.quality / 100f).background(Color(0xFFAD8AFF))) }
                        Text(" ${rec.quality}% • ${fmtDuration(durationMinutes(rec.bedtime, rec.wake_time))}", color = White, fontFamily = blenderBold(), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun fmtClock(minutes: Int): String {
    val m = ((minutes % 1440) + 1440) % 1440
    return "${(m / 60).toString().padStart(2, '0')}:${(m % 60).toString().padStart(2, '0')}"
}

private fun parseClockMinutes(s: String): Int {
    val parts = s.trim().split(":")
    val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 23
    val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
    return (h.coerceIn(0, 23)) * 60 + m.coerceIn(0, 59)
}

private fun fmtDuration(min: Int): String = when {
    min <= 0 -> "—"
    min % 60 == 0 -> "${min / 60} ч"
    min < 60 -> "$min мин"
    else -> "${min / 60} ч ${min % 60} мин"
}

/**
 * Recommended sleep time. Base = 8 h. Only well-rated nights (quality >= 80%) move it:
 * the recommendation follows the night with the best quality (on a tie - the more recent one).
 * 8 h/80% -> 8 h; 9 h/90% -> 9 h; 10 h/75% -> stays 9 h.
 */
private fun recommendedSleepMinutes(records: List<SleepRecord>): Int {
    val good = records.sortedBy { it.date }.takeLast(30).filter { it.quality >= 80 && durationMinutes(it.bedtime, it.wake_time) > 0 }
    if (good.isEmpty()) return 480
    val best = good.maxWithOrNull(compareBy<SleepRecord> { it.quality }.thenBy { it.date })!!
    return durationMinutes(best.bedtime, best.wake_time)
}

/** "Lie down before HH:MM": the latest bedtime among nights the user rated 80% or higher. */
private fun sleepBedtimeAdvice(records: List<SleepRecord>): String? {
    val good = records.sortedBy { it.date }.takeLast(30).filter { it.quality >= 80 && durationMinutes(it.bedtime, it.wake_time) > 0 }
    if (good.isEmpty()) return null
    // Bedtimes after midnight (before noon) count as "later" than evening ones.
    val latest = good.maxOf { val m = parseClockMinutes(it.bedtime); if (m < 12 * 60) m + 1440 else m }
    return fmtClock(latest)
}

private val twoDigits = (0..59).map { it.toString().padStart(2, '0') }

/** Vertical drum: scroll and release, the centered row becomes the value. */
@Composable
private fun WheelPicker(items: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, width: Dp = 64.dp) {
    val itemH = 34.dp
    val last = (items.size - 1).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, last))
    val fling = rememberSnapFlingBehavior(state)
    val centered by remember(items.size) {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }?.index ?: selectedIndex
        }
    }
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) onSelected(centered.coerceIn(0, last))
    }
    Box(Modifier.width(width).height(itemH * 3), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemH).background(Green.copy(alpha = 0.14f), RoundedCornerShape(8.dp)).border(1.dp, Green.copy(alpha = 0.6f), RoundedCornerShape(8.dp)))
        LazyColumn(state = state, flingBehavior = fling, contentPadding = PaddingValues(vertical = itemH), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items) { index, label ->
                Box(Modifier.fillMaxWidth().height(itemH), contentAlignment = Alignment.Center) {
                    Text(label, color = if (index == centered) White else Color.Gray, fontFamily = blenderBold(), fontSize = if (index == centered) 20.sp else 15.sp)
                }
            }
        }
    }
}

/** Time-of-day drum (24 h): hours 00-23 and minutes 00-59. */
@Composable
private fun TimeWheelDialog(title: String, initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var hour by remember { mutableStateOf(((initialMinutes % 1440) / 60).coerceIn(0, 23)) }
    var minute by remember { mutableStateOf((initialMinutes % 60).coerceIn(0, 59)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF050B10),
        shape = RoundedCornerShape(16.dp),
        title = { Text(title, color = Green, fontFamily = blenderBold(), fontSize = 17.sp) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    WheelPicker(twoDigits.take(24), hour, onSelected = { hour = it })
                    Text(":", color = Green, fontFamily = blenderBold(), fontSize = 22.sp)
                    WheelPicker(twoDigits, minute, onSelected = { minute = it })
                }
                Spacer(Modifier.height(6.dp))
                Text(fmtClock(hour * 60 + minute), color = Light, fontFamily = blenderBold(), fontSize = 14.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(hour * 60 + minute) }) { Text("Выбрать", color = Green, fontFamily = blenderBold()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Red) } }
    )
}

private fun moneyText(amount: Double): String {
    val value = amount.toLong()
    val digits = value.toString()
    val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped ₽"
}

@Composable
private fun FinanceWorkspace(vm: LifeViewModel) {
    LaunchedEffect(Unit) { vm.ensureFinanceAccount() }
    var entryType by remember { mutableStateOf("Расход") }
    var entryTitle by remember { mutableStateOf("") }
    var entryAmount by remember { mutableStateOf("") }
    var entryCategory by remember { mutableStateOf("Еда") }
    var selectedAccountId by remember(vm.data.finance_accounts.size) { mutableStateOf(vm.data.finance_accounts.firstOrNull()?.id.orEmpty()) }
    var showNewAccount by remember { mutableStateOf(false) }
    var accountName by remember { mutableStateOf("") }
    var accountOpening by remember { mutableStateOf("") }
    var editingAccountId by remember { mutableStateOf("") }
    var editAccountName by remember { mutableStateOf("") }
    var editAccountBalance by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val income = vm.data.finance_entries.filter { it.type == "Доход" }.sumOf { it.amount }
    val expenses = vm.data.finance_entries.filter { it.type != "Доход" }.sumOf { it.amount }
    fun balance(account: FinanceAccount): Double = account.opening_balance + vm.data.finance_entries.filter { it.account_id == account.id }.sumOf { if (it.type == "Доход") it.amount else -it.amount }
    val totalBalance = vm.data.finance_accounts.sumOf { balance(it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("МОИ ДЕНЬГИ", color = Green, fontFamily = blenderBold(), fontSize = 24.sp); Text("Счета, доходы и расходы", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp) }
            GreenButton("+ СЧЁТ") { showNewAccount = !showNewAccount }
        }
        if (showNewAccount) {
            Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("НОВЫЙ СЧЁТ", color = Light, fontFamily = blenderBold(), fontSize = 13.sp)
                    OutlinedTextField(value = accountName, onValueChange = { accountName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название счёта") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                    OutlinedTextField(value = accountOpening, onValueChange = { accountOpening = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' || ch == '-' } }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Начальный баланс, ₽") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                    GreenButton("СОЗДАТЬ СЧЁТ") { vm.addFinanceAccount(accountName, accountOpening.replace(',', '.').toDoubleOrNull() ?: 0.0); accountName = ""; accountOpening = ""; showNewAccount = false }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinanceMetric("РАСХОДЫ", moneyText(expenses), "за всё время", Color(0xFFFFA05A))
            FinanceMetric("БАЛАНС", moneyText(totalBalance), "на счетах", Light)
            FinanceMetric("ДОХОДЫ", moneyText(income), "за всё время", Color(0xFF79D4FF))
        }
        vm.data.finance_accounts.forEach { account ->
            Surface(Modifier.fillMaxWidth(), color = Color(0xFF087B47), border = BorderStroke(1.dp, Color(0xFF22C979)), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(35.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.17f)), contentAlignment = Alignment.Center) { Text("▤", color = White, fontSize = 19.sp) }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) { Text(account.name, color = White, fontFamily = blenderBold(), fontSize = 15.sp); Text(account.bank_name, color = White.copy(alpha = .75f), fontFamily = blenderBook(), fontSize = 10.sp) }
                        Text("✎", color = White, fontSize = 19.sp, modifier = Modifier.clickable { editingAccountId = account.id; editAccountName = account.name; editAccountBalance = account.opening_balance.toString() })
                    }
                    Text(moneyText(balance(account)), color = White, fontFamily = blenderBold(), fontSize = 28.sp)
                    Text("Текущий баланс с учётом операций", color = White.copy(alpha = .75f), fontFamily = blenderBook(), fontSize = 10.sp)
                    if (editingAccountId == account.id) {
                        OutlinedTextField(value = editAccountName, onValueChange = { editAccountName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                        OutlinedTextField(value = editAccountBalance, onValueChange = { editAccountBalance = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' || ch == '-' } }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Начальный баланс") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { GreenButton("СОХРАНИТЬ") { vm.updateFinanceAccount(account.copy(name = editAccountName.ifBlank { account.name }, opening_balance = editAccountBalance.replace(',', '.').toDoubleOrNull() ?: account.opening_balance)); editingAccountId = "" }; TextButton(onClick = { editingAccountId = "" }) { Text("Отмена", color = White) } }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FinanceShortcut("ЗАРПЛАТА", "₽", Color(0xFF28B879)) { entryType = "Доход"; entryTitle = "Зарплата"; entryCategory = "Зарплата" }
            FinanceShortcut("ИНВЕСТИЦИИ", "↗", Color(0xFF56B9DD)) { entryType = "Доход"; entryTitle = "Инвестиции"; entryCategory = "Инвестиции" }
            FinanceShortcut("КАТЕГОРИЯ", "+", Color(0xFFB4A0E8)) { entryCategory = ""; entryTitle = "" }
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ДОБАВИТЬ ОПЕРАЦИЮ", color = Green, fontFamily = blenderBold(), fontSize = 16.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Surface(Modifier.weight(1f).height(38.dp).clickable { entryType = "Доход" }, color = if (entryType == "Доход") Color(0xFF123B2A) else Color.Transparent, border = BorderStroke(1.dp, if (entryType == "Доход") Green else Grid), shape = RoundedCornerShape(7.dp), content = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("＋ ДОХОД", color = Light, fontFamily = blenderBold(), fontSize = 11.sp) } })
                    Surface(Modifier.weight(1f).height(38.dp).clickable { entryType = "Расход" }, color = if (entryType == "Расход") Color(0xFF3B1B1B) else Color.Transparent, border = BorderStroke(1.dp, if (entryType == "Расход") Red else Grid), shape = RoundedCornerShape(7.dp), content = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("− РАСХОД", color = Color(0xFFFF9797), fontFamily = blenderBold(), fontSize = 11.sp) } })
                }
                OutlinedTextField(value = entryTitle, onValueChange = { entryTitle = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название операции") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(value = entryAmount, onValueChange = { entryAmount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Сумма, ₽") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                    OutlinedTextField(value = entryCategory, onValueChange = { entryCategory = it }, modifier = Modifier.weight(1.4f), singleLine = true, label = { Text("Категория") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                }
                if (vm.data.finance_accounts.size > 1) {
                    var accountMenu by remember { mutableStateOf(false) }
                    Box {
                        GreenButton("СЧЁТ: ${vm.data.finance_accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "Основной"} ▾") { accountMenu = true }
                        DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) { vm.data.finance_accounts.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { selectedAccountId = a.id; accountMenu = false }) } }
                    }
                }
                if (error.isNotBlank()) Text(error, color = Red, fontFamily = blenderBook(), fontSize = 10.sp)
                GreenButton("СОХРАНИТЬ ОПЕРАЦИЮ") {
                    val amount = entryAmount.replace(',', '.').toDoubleOrNull()
                    if (amount == null || amount <= 0) error = "Укажите сумму больше нуля." else {
                        vm.addFinanceEntry(entryType, entryTitle, entryCategory, amount, selectedAccountId)
                        entryTitle = ""; entryAmount = ""; error = ""
                    }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ИСТОРИЯ ОПЕРАЦИЙ", color = Green, fontFamily = blenderBold(), fontSize = 16.sp)
                val records = vm.data.finance_entries.sortedByDescending { it.date }
                if (records.isEmpty()) Text("Операций пока нет. Добавьте доход или расход выше.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 11.sp)
                records.take(20).forEach { entry ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(if (entry.type == "Доход") Color(0xFF103522) else Color(0xFF321819)), contentAlignment = Alignment.Center) { Text(if (entry.type == "Доход") "↗" else "↘", color = if (entry.type == "Доход") Light else Red, fontSize = 15.sp) }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) { Text(entry.title, color = White, fontFamily = blenderBook(), fontSize = 12.sp, maxLines = 1); Text("${entry.category} • ${entry.date}", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp) }
                        Text((if (entry.type == "Доход") "+" else "−") + moneyText(entry.amount), color = if (entry.type == "Доход") Light else Color(0xFFFF8989), fontFamily = blenderBold(), fontSize = 11.sp)
                        Text("×", color = Red, modifier = Modifier.clickable { vm.deleteFinanceEntry(entry.id) }.padding(start = 8.dp))
                    }
                    HorizontalDivider(color = Color(0xFF203325), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun FinanceShortcut(title: String, symbol: String, tint: Color, onClick: () -> Unit) {
    Surface(Modifier.width(112.dp).clickable(onClick = onClick), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(9.dp)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(29.dp).clip(RoundedCornerShape(7.dp)).background(tint.copy(alpha = 0.18f)).border(1.dp, tint, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                Text(symbol, color = tint, fontFamily = blenderBold(), fontSize = 16.sp)
            }
            Text(title, color = White, fontFamily = blenderBold(), fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun FinanceMetric(title: String, amount: String, subtitle: String, tint: Color) {
    Surface(color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), horizontalAlignment = Alignment.Start) {
            Text(title, color = Color.Gray, fontFamily = blenderBold(), fontSize = 9.sp)
            Text(amount, color = tint, fontFamily = blenderBold(), fontSize = 18.sp, maxLines = 1)
            Text(subtitle, color = Color.Gray, fontFamily = blenderBook(), fontSize = 8.sp)
        }
    }
}

@Composable
private fun FinanceChartsWorkspace(vm: LifeViewModel) {
    var budgetCategory by remember { mutableStateOf("") }
    var budgetLimit by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val income = vm.data.finance_entries.filter { it.type == "Доход" }.sumOf { it.amount }
    val expenses = vm.data.finance_entries.filter { it.type != "Доход" }.sumOf { it.amount }
    val balance = vm.data.finance_accounts.sumOf { account -> account.opening_balance + vm.data.finance_entries.filter { it.account_id == account.id }.sumOf { if (it.type == "Доход") it.amount else -it.amount } }
    val spendByCategory = vm.data.finance_entries.filter { it.type != "Доход" }.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val displayCategories = (vm.data.finance_budgets.map { it.category } + spendByCategory.keys).distinct().ifEmpty { listOf("Жильё", "Техника", "Еда", "Одежда", "Транспорт") }
    val palette = listOf(Color(0xFFFFB52E), Color(0xFFE46F7B), Color(0xFF24B78D), Color(0xFF2CBEA2), Color(0xFF5FABB6), Color(0xFF9A8BE5))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("СТАТИСТИКА ФИНАНСОВ", color = Green, fontFamily = blenderBold(), fontSize = 23.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FinanceMetric("ДОХОДЫ", "+ ${moneyText(income)}", "за всё время", Color(0xFF54BC8B))
            FinanceMetric("РАСХОДЫ", "− ${moneyText(expenses)}", "за всё время", Color(0xFFE77A87))
            FinanceMetric("БАЛАНС", moneyText(balance), "текущий", Color(0xFF60B9E5))
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ДОХОДЫ И РАСХОДЫ", color = Green, fontFamily = blenderBold(), fontSize = 16.sp)
                val entriesByDate = vm.data.finance_entries.groupBy { it.date }.toSortedMap().toList().takeLast(7)
                if (entriesByDate.isEmpty()) Text("График заполнится после первых операций.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                else {
                    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                        val left = 10.dp.toPx(); val right = size.width - 10.dp.toPx(); val top = 10.dp.toPx(); val bottom = size.height - 22.dp.toPx()
                        val maxVal = entriesByDate.maxOf { (_, list) -> maxOf(list.filter { it.type == "Доход" }.sumOf { it.amount }, list.filter { it.type != "Доход" }.sumOf { it.amount }) }.coerceAtLeast(1.0)
                        for (i in 0..4) { val y = top + (bottom - top) * i / 4f; drawLine(Color(0xFF26302A), androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(right, y), 1.dp.toPx()) }
                        val groupWidth = (right - left) / entriesByDate.size
                        entriesByDate.forEachIndexed { index, (_, list) ->
                            val x = left + groupWidth * index + groupWidth * 0.18f
                            val inc = list.filter { it.type == "Доход" }.sumOf { it.amount }
                            val exp = list.filter { it.type != "Доход" }.sumOf { it.amount }
                            val h1 = (bottom - top) * (inc / maxVal).toFloat()
                            val h2 = (bottom - top) * (exp / maxVal).toFloat()
                            drawRect(Color(0xFF29AD78), androidx.compose.ui.geometry.Offset(x, bottom - h1), androidx.compose.ui.geometry.Size(groupWidth * .24f, h1))
                            drawRect(Color(0xFFE16B77), androidx.compose.ui.geometry.Offset(x + groupWidth * .29f, bottom - h2), androidx.compose.ui.geometry.Size(groupWidth * .24f, h2))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Text("● Доходы", color = Color(0xFF29AD78), fontFamily = blenderBook(), fontSize = 9.sp); Text("● Расходы", color = Color(0xFFE16B77), fontFamily = blenderBook(), fontSize = 9.sp) }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("БЮДЖЕТЫ И КАТЕГОРИИ", color = Green, fontFamily = blenderBold(), fontSize = 16.sp)
                displayCategories.forEachIndexed { index, category ->
                    val spent = spendByCategory[category] ?: 0.0
                    val budget = vm.data.finance_budgets.firstOrNull { it.category.equals(category, ignoreCase = true) }
                    val limit = budget?.limit_amount ?: 0.0
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(category, color = White, fontFamily = blenderBook(), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(moneyText(spent), color = White, fontFamily = blenderBold(), fontSize = 11.sp)
                                Text(if (limit > 0) "лимит ${moneyText(limit)}" else "лимит не задан", color = Color.Gray, fontFamily = blenderBook(), fontSize = 8.sp)
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(17.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF191F22))) {
                            if (limit > 0) Box(Modifier.fillMaxHeight().fillMaxWidth((spent / limit).toFloat().coerceIn(0f, 1f)).background(palette[index % palette.size]))
                        }
                    }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("НАСТРОИТЬ БЮДЖЕТ", color = Green, fontFamily = blenderBold(), fontSize = 15.sp)
                OutlinedTextField(value = budgetCategory, onValueChange = { budgetCategory = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Категория расходов") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                OutlinedTextField(value = budgetLimit, onValueChange = { budgetLimit = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Лимит на период, ₽") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                if (message.isNotBlank()) Text(message, color = Light, fontFamily = blenderBook(), fontSize = 10.sp)
                GreenButton("СОХРАНИТЬ БЮДЖЕТ") {
                    val limit = budgetLimit.replace(',', '.').toDoubleOrNull()
                    if (budgetCategory.isBlank() || limit == null || limit <= 0) message = "Укажите категорию и лимит больше нуля." else {
                        vm.upsertFinanceBudget(budgetCategory.trim(), limit); message = "Бюджет сохранён"; budgetCategory = ""; budgetLimit = ""
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeatureCard(title: String, description: String, onAction: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Green, fontFamily = blenderBold(), fontSize = 19.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(description, color = Color.Gray, fontFamily = blenderBook(), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            GreenButton("ОТКРЫТЬ РАЗДЕЛ") { onAction() }
        }
    }
}


@Composable
private fun CurrentTimeText(vm:LifeViewModel,color: Color,fontSize: androidx.compose.ui.unit.TextUnit,onClick:()->Unit) {
    var now by remember(vm.timeOffsetMinutes) { mutableStateOf(vm.formattedAppNow()) }
    LaunchedEffect(vm.timeOffsetMinutes) {
        while (true) {
            now = vm.formattedAppNow()
            delay(1.seconds)
        }
    }
    Text(now,color=color,fontFamily=blenderBook(),fontSize=fontSize,maxLines=1,modifier=Modifier.clickable(onClick=onClick))
}

@Composable private fun Header(vm:LifeViewModel){
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically){
            MenuButton(58.dp, isOpen = vm.menuOpen) { vm.menuOpen = !vm.menuOpen }
            Spacer(Modifier.width(8.dp))
            ProfileAvatar(vm, 48.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.width(220.dp)){
                Column(Modifier.clickable{vm.settingsOpen=true}){
                    Text(vm.data.display_name.ifBlank{vm.username},color=White,fontFamily=blenderBold(),fontSize=16.sp,maxLines=1)
                    Text("Роль: ${vm.data.role}",color=Light,fontFamily=blenderBook(),fontSize=11.sp,maxLines=1)
                }
                CurrentTimeText(vm,color=Color.Gray,fontSize=10.sp,onClick={vm.timeTravelOpen=true})
            }
            Spacer(Modifier.weight(1f))
            val unread = vm.unread()
            OutlineIconButton(IconKind.BELL,if(unread>0)unread.toString() else ""){vm.notificationsOpen=true}
        }
        // Put LVL/REP below the profile row so the drawer's horizontal edge can
        // sit just above their outlines without crossing the avatar or time.
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(334.dp))
            Stats(vm)
        }
    }
}

@Composable private fun Stats(vm:LifeViewModel){
    Row(Modifier.width(280.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        LevelWaterCard(vm,Modifier.weight(1f))
        ReputationCard(vm,Modifier.weight(1f))
    }
}

@Composable private fun ProgressBar(v:Float,base:Color,fill:Color){Box(Modifier.width(172.dp).height(6.dp).background(base)){Box(Modifier.fillMaxHeight().fillMaxWidth(v.coerceIn(0f,1f)).background(fill))}}

@Composable private fun MainContent(vm:LifeViewModel){
    if(vm.activeTab=="СТАТИСТИКА"){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("ОБЩАЯ СТАТИСТИКА",color=Green,fontFamily=blenderBold(),fontSize=28.sp)
            StatisticsPanel(vm)
            OverallTrainingProgressPentagon(vm)
        }
        return
    }
    if(vm.activeTab=="ДОСТИЖЕНИЯ"){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("ДОСТИЖЕНИЯ",color=Green,fontFamily=blenderBold(),fontSize=28.sp)
            AchievementsPanel(vm)
        }
        return
    }
    if(vm.activeTab=="ЗДОРОВЬЕ" || vm.activeTab=="ТРЕНИРОВКИ"){
        HealthPanel(vm)
        return
    }
    when (vm.activeTab) {
        "ЗАМЕТКИ" -> { NotesWorkspace(vm); return }
        "СПИСОК ЗАМЕТОК" -> { NotesListWorkspace(vm); return }
        "ПЛАНЫ В ПОДРОБНОСТЯХ", "ПЛАНЫ" -> { DetailedPlansWorkspace(vm); return }
        "КАРТА ПЛАНОВ" -> { PlansMapWorkspace(vm); return }
        "ФИН. РАСЧЕТ" -> { FinanceWorkspace(vm); return }
        "ГРАФИКИ" -> { FinanceChartsWorkspace(vm); return }
    }
    if(vm.activeTab!="ЗАДАЧИ"){Box(Modifier.fillMaxSize().border(1.dp,Grid),contentAlignment=Alignment.Center){Text(vm.activeTab,color=Green,fontFamily=blenderBold(),fontSize=28.sp);}
        return
    }
    Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(10.dp)){TaskPanel(vm,Modifier.weight(1.25f));Workload(vm,Modifier.weight(.7f));CalendarPanel(vm,Modifier.weight(1f))}
}

@Composable private fun TaskPanel(vm:LifeViewModel,modifier:Modifier = Modifier){
    Panel(vm,modifier){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("ЗАДАЧИ",color=Green,fontFamily=blenderBold(),fontSize=24.sp)
            Spacer(Modifier.weight(1f))
            CreateTaskButton{vm.openCreate()}
        }
        Row{
            FilterButton("АКТИВНЫЕ",vm.taskView=="ACTIVE"){vm.taskView="ACTIVE"}
            FilterButton("ЗАВЕРШЕННЫЕ",vm.taskView=="DONE"){vm.taskView="DONE"}
        }
        Spacer(Modifier.height(8.dp))
        val list=vm.tasks.filter{it.date==vm.selectedTaskDate&&it.done==(vm.taskView=="DONE")}
        if(list.isEmpty())Text("Нет задач",color=Color.Gray,fontFamily=blenderBook())
        list.forEach{TaskRow(vm,it)}
    }
}
@Composable private fun GreenButton(text:String,onClick:()->Unit){Button(onClick=onClick,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF071507),contentColor=White),border=BorderStroke(2.dp,Green),shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)){Text(text,fontFamily=blenderBold(),fontSize=12.sp)}}
@Composable private fun CreateTaskButton(onClick:()->Unit){Button(onClick=onClick,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF071507),contentColor=White),border=BorderStroke(1.5.dp,Green),shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=9.dp,vertical=5.dp),modifier=Modifier.height(34.dp)){Text("+ СОЗДАТЬ",fontFamily=blenderBold(),fontSize=11.sp)}}
@Composable private fun FilterButton(t:String,on:Boolean,click:()->Unit){TextButton(onClick=click,contentPadding=PaddingValues(horizontal=7.dp)){Text(t,color=if(on)Green else Color.Gray,fontFamily=blenderBold(),fontSize=12.sp)}}
@Composable private fun TaskRow(vm:LifeViewModel,t:TaskItem){
    val focused = vm.focusedTask === t
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp).background(if(focused) Color(0xFF082D19) else Color.Transparent, RoundedCornerShape(8.dp)).padding(horizontal=3.dp, vertical=2.dp),verticalAlignment=Alignment.CenterVertically){
        Checkbox(t.done,{if(it)vm.complete(t)else vm.restore(t)},colors=CheckboxDefaults.colors(checkedColor=Green))
        Column(Modifier.weight(1f)){
            Text(t.text,color=if(t.done)Color.Gray else White,fontFamily=blenderBook(),fontSize=14.sp,textDecoration=if(t.done)TextDecoration.LineThrough else null)
            if(t.description.isNotBlank())Text(t.description,color=Color(0xFFAAAAAA),fontFamily=blenderBook(),fontSize=10.sp,maxLines=2)
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("${t.start_time} • ${t.duration_minutes} мин",color=Color.Gray,fontFamily=blenderBook(),fontSize=11.sp)
                Spacer(Modifier.width(8.dp))
                DifficultyChip(t.difficulty)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("+${t.points} XP",color=Light,fontFamily=blenderBold(),fontSize=21.sp,maxLines=1)
        TextButton(onClick={vm.openEdit(t)}){Text("Изменить",color=Light,fontSize=11.sp)}
        TextButton(onClick={vm.delete(t)}){Text("Удалить",color=Red,fontSize=11.sp)}
    }
}
@Composable private fun Workload(vm:LifeViewModel,modifier:Modifier = Modifier){
    Panel(vm,modifier){
        Text("ЗАГРУЖЕННОСТЬ",color=Green,fontFamily=blenderBold(),fontSize=17.sp)
        val mins=vm.tasks.filter{it.date==vm.selectedTaskDate}.sumOf{it.duration_minutes}
        Text("${mins/60} ч ${mins%60} мин",color=White,fontFamily=blenderBold(),fontSize=28.sp,modifier=Modifier.padding(vertical=25.dp))
        Text(if(mins>960)"Время сокращено на ${(mins-960)/60} ч" else "Норма сна: 8 часов",color=if(mins>960)Red else Light,fontFamily=blenderBook())
        Spacer(Modifier.height(15.dp))
        Text("Лимит работы: 16 часов",color=Color.Gray,fontFamily=blenderBook())
    }
}

@Composable private fun CalendarPanel(vm:LifeViewModel,modifier:Modifier = Modifier){
    Panel(vm,modifier){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("КАЛЕНДАРЬ",color=Green,fontFamily=blenderBold(),fontSize=24.sp)
            Spacer(Modifier.weight(1f))
            FilterButton("ОБЫЧНЫЙ",!vm.calendarR){vm.calendarR=false}
            FilterButton("РАБОЧИЙ",vm.calendarR){vm.calendarR=true}
        }
        Spacer(Modifier.height(8.dp))
        CalendarGrid(vm)
    }
}

private fun isLeapYear(year: Int): Boolean =
    year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 30
}

private fun toDisplayDate(iso: String): String {
    val parts = iso.split("-")
    return if (parts.size == 3 && parts[0].length == 4) "${parts[2]}-${parts[1]}-${parts[0]}" else iso
}

private fun fromDisplayDate(value: String): String? {
    val parts = value.trim().split("-")
    if (parts.size != 3 || parts[0].length != 2 || parts[1].length != 2 || parts[2].length != 4) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return try { LocalDate(year, month, day).toString() } catch (_: Throwable) { null }
}

private fun shiftMonth(date: LocalDate, delta: Int): LocalDate {
    var year = date.year
    var month = date.month.ordinal + 1 + delta
    while (month < 1) { month += 12; year -= 1 }
    while (month > 12) { month -= 12; year += 1 }
    return LocalDate(year, month, 1)
}

@Composable
private fun CalendarGrid(vm: LifeViewModel) {
    val selected = if(vm.calendarR) vm.selectedWorkDate else vm.selectedTaskDate
    val date = LocalDate.parse(selected)
    val year = date.year
    val month = date.month.ordinal + 1
    val first = LocalDate(year, month, 1)
    val days = daysInMonth(year, month)
    val firstWeekday = first.dayOfWeek.ordinal + 1
    val weekdays=listOf("ПН","ВТ","СР","ЧТ","ПТ","СБ","ВС")

    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        TextButton(onClick={
            val next=shiftMonth(date,-1).toString()
            if(vm.calendarR)vm.selectedWorkDate=next else vm.selectedTaskDate=next
        }){Text("‹",fontSize=26.sp,color=Green)}
        Spacer(Modifier.weight(1f))
        Text("${month}/${year}",color=White,fontFamily=blenderBold(),fontSize=16.sp)
        Spacer(Modifier.weight(1f))
        TextButton(onClick={
            val next=shiftMonth(date,1).toString()
            if(vm.calendarR)vm.selectedWorkDate=next else vm.selectedTaskDate=next
        }){Text("›",fontSize=26.sp,color=Green)}
    }

    Row(Modifier.fillMaxWidth()){
        weekdays.forEach{Text(it,color=Color.Gray,fontFamily=blenderBold(),fontSize=8.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
    }
    Spacer(Modifier.height(3.dp))

    Column {
        for(week in 0..5){
            Row(Modifier.fillMaxWidth()){
                for(col in 1..7){
                    val index=week*7+col
                    val day=index-(firstWeekday-1)
                    if(day in 1..days){
                        val iso=LocalDate(year,month,day).toString()
                        val isSelected=iso==selected
                        val mark=vm.data.calendar_marks[iso]
                        val created=vm.createdCount(iso)
                        val completed=vm.completedCountOn(iso)
                        Box(
                            Modifier.weight(1f).height(42.dp).padding(2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if(isSelected)Color(0xFF79C8EE).copy(alpha=.22f) else Color.Transparent)
                                .clickable{if(vm.calendarR)vm.selectedWorkDate=iso else vm.selectedTaskDate=iso},
                            contentAlignment=Alignment.Center
                        ){
                            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                                Text(day.toString(),color=if(isSelected)Color(0xFFB9EAFF) else White,fontFamily=blenderBold(),fontSize=11.sp)
                                if(vm.calendarR){
                                    Text(mark?:"",color=if(mark=="Р")Red else Green,fontFamily=blenderBold(),fontSize=8.sp,maxLines=1)
                                }else{
                                    Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){
                                        if(created>0)Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF67B9FF)))
                                        if(completed>0)Text("✓",color=Green,fontFamily=blenderBold(),fontSize=7.sp)
                                    }
                                }
                            }
                        }
                    }else Spacer(Modifier.weight(1f).height(42.dp))
                }
            }
        }
    }

    if(vm.calendarR){
        Row(Modifier.fillMaxWidth().padding(top=7.dp),horizontalArrangement=Arrangement.Center){
            Button(onClick={vm.toggleWorkMark(vm.selectedWorkDate,"Р")},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFF3333),contentColor=Black),contentPadding=PaddingValues(horizontal=14.dp,vertical=4.dp)){Text("Р",color=Black,fontFamily=blenderBold(),fontSize=11.sp)}
            Spacer(Modifier.width(8.dp))
            Button(onClick={vm.toggleWorkMark(vm.selectedWorkDate,"")},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFD9DDE2),contentColor=Color(0xFF25282C)),contentPadding=PaddingValues(horizontal=14.dp,vertical=4.dp)){Text("О",color=Color(0xFF25282C),fontFamily=blenderBold(),fontSize=11.sp)}
        }
        Text("Рабочий календарь: ${vm.selectedWorkDate} • ${vm.data.calendar_marks[vm.selectedWorkDate] ?: "не отмечена"}",color=White,fontFamily=blenderBook(),fontSize=10.sp,modifier=Modifier.padding(top=7.dp))
    }
}

@Composable private fun Panel(vm:LifeViewModel,modifier:Modifier = Modifier,content:@Composable ColumnScope.()->Unit){Surface(modifier.padding(2.dp),color=Color(0xFF050B10).copy(alpha=(vm.data.block_opacity*0.72).toFloat()),border=BorderStroke(4.dp,Grid),shape=RoundedCornerShape(0.dp)){Column(Modifier.padding(14.dp)) { content() }}}

@Composable private fun TaskEditor(vm:LifeViewModel,mode:EditorMode){
    val existing=vm.editorTask
    var text by remember(existing){mutableStateOf(existing?.text?:"")}
    var description by remember(existing){mutableStateOf(existing?.description?:"")}
    var time by remember(existing){mutableStateOf(existing?.start_time?:"8:00 AM")}
    var hours by remember(existing){mutableStateOf(((existing?.duration_minutes?:30)/60).toString())}
    var minutes by remember(existing){mutableStateOf(((existing?.duration_minutes?:30)%60).toString())}
    var diff by remember(existing){mutableStateOf(existing?.difficulty?:"Легкий")}
    var date by remember(existing){mutableStateOf(toDisplayDate(existing?.date?:vm.selectedTaskDate))}
    var remind by remember(existing){mutableStateOf(existing?.remind?:false)}
    var error by remember(existing){mutableStateOf("")}
    AlertDialog(
        onDismissRequest={vm.closeEditor()},
        containerColor=Color(0xFF050B10),
        titleContentColor=Green,
        textContentColor=White,
        shape=RoundedCornerShape(16.dp),
        title={Text(if(mode==EditorMode.CREATE)"СОЗДАТЬ ЗАДАЧУ" else "РЕДАКТИРОВАТЬ ЗАДАЧУ",fontFamily=blenderBold(),color=Green)},
        text={Column(Modifier.verticalScroll(rememberScrollState())){
            EditorField("Задача",text){text=it}
            EditorField("Описание",description){description=it}
            EditorField("Время (AM/PM)",time){time=it}
            EditorField("Дата (dd-mm-yyyy)",date){date=it}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                EditorField("Часы",hours,Modifier.weight(1f)){hours=it.filter(Char::isDigit)}
                EditorField("Минуты",minutes,Modifier.weight(1f)){minutes=it.filter(Char::isDigit)}
            }
            Text("Сложность",color=Light,fontFamily=blenderBook(),modifier=Modifier.padding(top=8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Легкий","Средний","Тяжелый","Экстремальный").forEach{d->FilterButton(d,diff==d){diff=d}}}
            DifficultyChip(diff)
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(remind,{remind=it},colors=CheckboxDefaults.colors(checkedColor=Green));Text("Напомнить",color=White,fontFamily=blenderBook())}
            if(error.isNotBlank()) Text(error,color=Red,fontFamily=blenderBook(),fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
        }},
        confirmButton={TextButton(onClick={
            val h=hours.toIntOrNull()?:0
            val m=(minutes.toIntOrNull()?:0).coerceIn(0,59)
            val displayDate=fromDisplayDate(date)
            if(text.isBlank()){error="Введите название задачи";return@TextButton}
            if(displayDate==null){error="Дата должна быть в формате dd-mm-yyyy";return@TextButton}
            val dur=(h.coerceAtLeast(0)*60)+m
            if(dur<=0){error="Длительность должна быть больше 0 минут";return@TextButton}
            if(mode==EditorMode.CREATE){vm.addTask(text,description,time,dur,diff,displayDate,remind,"","")} else existing?.let{vm.editTask(it,text,description,time,dur,diff,displayDate,remind,"","")}
        }){Text("Сохранить",color=Green,fontFamily=blenderBold())}},
        dismissButton={TextButton(onClick={vm.closeEditor()}){Text("Отмена",color=Red)}}
    )
}
@Composable private fun EditorField(label:String,value:String,modifier:Modifier=Modifier,onChange:(String)->Unit){OutlinedTextField(value,onChange,label={Text(label)},singleLine=true,modifier=modifier.fillMaxWidth().padding(vertical=4.dp),colors=darkFields())}

@Composable
private fun TrainingCreatorDialog(vm:LifeViewModel){
    var name by remember(vm.trainingCreatorGroup,vm.trainingCreatorDate){mutableStateOf("")}
    var description by remember(vm.trainingCreatorGroup,vm.trainingCreatorDate){mutableStateOf("")}
    var error by remember(vm.trainingCreatorGroup,vm.trainingCreatorDate){mutableStateOf("")}
    val group=vm.trainingCreatorGroup
    val date=vm.trainingCreatorDate
    val difficulty=vm.trainingDifficultyForGroup(group,date)
    AlertDialog(
        onDismissRequest={vm.closeTrainingCreator()},
        containerColor=Color(0xFF050B10),
        titleContentColor=Green,
        textContentColor=White,
        shape=RoundedCornerShape(16.dp),
        title={Text("СОЗДАТЬ ТРЕНИРОВКУ",color=Green,fontFamily=blenderBold(),fontSize=18.sp)},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Text("Группа: $group",color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=11.sp)
            Text("Дата: ${toDisplayDate(date)}",color=Color.Gray,fontFamily=blenderBook(),fontSize=10.sp)
            Text("Сложность определяется автоматически: $difficulty",color=Color(0xFF9FE7FF),fontFamily=blenderBook(),fontSize=10.sp)
            EditorField("Название упражнения",name){name=it}
            EditorField("Описание тренировки",description){description=it}
            if(error.isNotBlank())Text(error,color=Red,fontFamily=blenderBook(),fontSize=10.sp)
        }},
        confirmButton={TextButton(onClick={
            if(name.isBlank()){error="Введите название упражнения";return@TextButton}
            vm.addCustomTraining(group,date,name,description)
        }){Text("Сохранить",color=Green,fontFamily=blenderBold())}},
        dismissButton={TextButton(onClick={vm.closeTrainingCreator()}){Text("Отмена",color=Red)}}
    )
}

@Composable
private fun NotificationCenterDialog(vm:LifeViewModel){
    val notifications = vm.data.notifications.toList().asReversed()
    AlertDialog(
        onDismissRequest={vm.notificationsOpen=false},
        containerColor=Color(0xFF050B10),
        titleContentColor=Green,
        textContentColor=White,
        shape=RoundedCornerShape(16.dp),
        title={Text("УВЕДОМЛЕНИЯ",color=Green,fontFamily=blenderBold())},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=420.dp).verticalScroll(rememberScrollState())){
                if(notifications.isEmpty()){
                    Text("Нет уведомлений",color=Color.Gray,fontFamily=blenderBook(),fontSize=13.sp)
                }else{
                    notifications.forEach { n ->
                        val isUnread = !vm.data.read_notifications.contains(n)
                        Surface(
                            Modifier.fillMaxWidth().padding(vertical=3.dp).clickable{vm.onNotificationClicked(n)},
                            color=if(isUnread) Color(0xFF0A2014) else Color(0xFF0A0F13),
                            border=BorderStroke(1.dp,if(isUnread)Green.copy(alpha=.75f) else Color(0xFF20302A)),
                            shape=RoundedCornerShape(9.dp)
                        ){
                            Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                                Box(Modifier.size(6.dp).clip(CircleShape).background(if(isUnread)Green else Color.DarkGray))
                                Spacer(Modifier.width(8.dp))
                                Text(n,color=if(isUnread)White else Color.Gray,fontFamily=blenderBook(),fontSize=12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton={
            Row(horizontalArrangement=Arrangement.End){
                TextButton(onClick={vm.markReadAll()}){Text("Прочитать всё",color=Light,fontFamily=blenderBold(),fontSize=11.sp)}
                TextButton(onClick={vm.notificationsOpen=false}){Text("Закрыть",color=Green,fontFamily=blenderBold())}
            }
        }
    )
}

@Composable
private fun TimeTravelDialog(vm:LifeViewModel){
    AlertDialog(
        onDismissRequest={vm.timeTravelOpen=false},
        containerColor=Color(0xFF050B10),
        titleContentColor=Green,
        textContentColor=White,
        shape=RoundedCornerShape(16.dp),
        title={Text("ПЕРЕМОТКА ВРЕМЕНИ",color=Green,fontFamily=blenderBold())},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text(vm.formattedAppNow(),color=Color(0xFFB9EAFF),fontFamily=blenderBold(),fontSize=18.sp)
                Text(vm.timeOffsetText(),color=Color.Gray,fontFamily=blenderBook(),fontSize=11.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    GreenButton("−24ч"){vm.shiftTestTime(-1440)}
                    GreenButton("−1ч"){vm.shiftTestTime(-60)}
                    GreenButton("−15м"){vm.shiftTestTime(-15)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    GreenButton("+15м"){vm.shiftTestTime(15)}
                    GreenButton("+1ч"){vm.shiftTestTime(60)}
                    GreenButton("+24ч"){vm.shiftTestTime(1440)}
                }
            }
        },
        confirmButton={TextButton(onClick={vm.resetTestTime()}){Text("Сбросить",color=Light,fontFamily=blenderBold())}},
        dismissButton={TextButton(onClick={vm.timeTravelOpen=false}){Text("Закрыть",color=Color.Gray,fontFamily=blenderBold())}}
    )
}

@Composable private fun SettingsDialog(vm:LifeViewModel, onLogout:()->Unit){
    var tab by remember{mutableStateOf("Личные данные")}
    AlertDialog(
        onDismissRequest={vm.settingsOpen=false},
        containerColor=Color(0xFF050B10),
        titleContentColor=Green,
        textContentColor=White,
        shape=RoundedCornerShape(16.dp),
        title={Text("НАСТРОЙКИ",color=Green,fontFamily=blenderBold())},
        text={Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("Личные данные","Внешний вид","Уведомления").forEach{t->FilterButton(t,tab==t){tab=t}}
            }
            Spacer(Modifier.height(8.dp))
            when(tab){
                "Личные данные"->ProfileSettings(vm,onLogout)
                "Внешний вид"->AppearanceSettings(vm)
                "Уведомления"->NotificationSettings(vm)
                else->ProfileSettings(vm,onLogout)
            }
        }},
        confirmButton={TextButton(onClick={vm.settingsOpen=false}){Text("Закрыть",color=Green,fontFamily=blenderBold())}}
    )
}
@Composable private fun ProfileSettings(vm:LifeViewModel, onLogout:()->Unit){
    var name by remember{mutableStateOf(vm.data.display_name)}
    var login by remember{mutableStateOf(vm.username)}
    var pass by remember{mutableStateOf("")}
    EditorField("Никнейм",name){name=it}
    EditorField("Логин",login){login=it}
    EditorField("Новый пароль",pass){pass=it}
    Row(verticalAlignment=Alignment.CenterVertically){
        Text("Аватар",color=White,fontFamily=blenderBook(),modifier=Modifier.weight(1f))
        GreenButton("Выбрать"){pickImage{vm.setPhoto(it)}}
    }
    Spacer(Modifier.height(6.dp))
    Button(onClick={vm.updateProfile(name,login,pass);vm.settingsOpen=false},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF071507)),border=BorderStroke(1.dp,Green)){Text("Сохранить личные данные",color=White,fontFamily=blenderBold())}
    Spacer(Modifier.height(6.dp))
    Button(onClick={ vm.logout(); onLogout() },colors=ButtonDefaults.buttonColors(containerColor=Color.Red)){Text("ВЫЙТИ ИЗ АККАУНТА",color=Color.White,fontFamily=blenderBold())}
}
@Composable private fun AppearanceSettings(vm:LifeViewModel){var bg by remember{mutableFloatStateOf(vm.data.background_opacity.toFloat())};var block by remember{mutableFloatStateOf(vm.data.block_opacity.toFloat())};var blur by remember{mutableFloatStateOf(vm.data.background_blur.toFloat())};Text("Фон",color=Light,fontFamily=blenderBold());Row(verticalAlignment=Alignment.CenterVertically){GreenButton("Выбрать фон"){pickImage{vm.setBackground(it)}}
        Spacer(Modifier.width(8.dp))
        GreenButton("Сбросить"){vm.resetBackground()}
        Spacer(Modifier.width(8.dp))
        Text(if(vm.data.background.isBlank())"Исходный fon.png" else "Пользовательский фон",color=Color.Gray,fontFamily=blenderBook(),fontSize=11.sp)};Text("Прозрачность фона",color=White,fontFamily=blenderBook());Slider(bg,{bg=it;vm.setAppearance(bg.toDouble(),block.toDouble(),blur.toDouble())},valueRange=.1f..1f);Text("Прозрачность блоков",color=White,fontFamily=blenderBook());Slider(block,{block=it;vm.setAppearance(bg.toDouble(),block.toDouble(),blur.toDouble())},valueRange=.1f..1f);Text("Размытие фона",color=White,fontFamily=blenderBook());Slider(blur,{blur=it;vm.setAppearance(bg.toDouble(),block.toDouble(),blur.toDouble())},valueRange=0f..20f)}
@Composable private fun NotificationSettings(vm:LifeViewModel){Row(verticalAlignment=Alignment.CenterVertically){Text("Уведомления",color=White,fontFamily=blenderBook(),modifier=Modifier.weight(1f));Switch(vm.data.notifications_enabled,{vm.setNotifications(it)},colors=SwitchDefaults.colors(checkedThumbColor=Green,checkedTrackColor=DarkBlue))};Text("Проверка времени выполняется автоматически. При приближении задачи появляется уведомление.",color=Color.Gray,fontFamily=blenderBook(),fontSize=11.sp,modifier=Modifier.padding(top=8.dp));Spacer(Modifier.height(10.dp));Text("Непрочитано: ${vm.unread()}",color=Light,fontFamily=blenderBook())}

@Composable private fun ConfirmDialog(d:DialogState,vm:LifeViewModel){AlertDialog(onDismissRequest={vm.dialog=null},containerColor=Color(0xFF050B10),titleContentColor=Green,textContentColor=White,shape=RoundedCornerShape(16.dp),title={Text(d.title,color=Green,fontFamily=blenderBold())},text={Text(d.text,color=White,fontFamily=blenderBook())},confirmButton={TextButton(onClick={d.onYes();vm.dialog=null}){Text("Подтвердить",color=Green)}},dismissButton={TextButton(onClick={vm.dialog=null}){Text("Отмена",color=Red)}})}


// ===== Карта планов =====
private data class MapNode(val plan: DetailedPlanItem, val level: Int, val angleDeg: Double)

private fun computePlanMap(plans: List<DetailedPlanItem>): List<MapNode> {
    val center = plans.firstOrNull { it.is_central } ?: return emptyList()
    val byId = plans.associateBy { it.id }
    fun neighbors(p: DetailedPlanItem): List<DetailedPlanItem> =
        (p.linked_plan_ids.mapNotNull { byId[it] } + plans.filter { it.linked_plan_ids.contains(p.id) }).distinctBy { it.id }
    val dist = mutableMapOf(center.id to 0)
    val children = mutableMapOf<String, MutableList<String>>()
    val queue = mutableListOf(center)
    var qi = 0
    while (qi < queue.size) {
        val cur = queue[qi++]
        for (n in neighbors(cur)) {
            if (!dist.containsKey(n.id)) {
                dist[n.id] = dist.getValue(cur.id) + 1
                children.getOrPut(cur.id) { mutableListOf() }.add(n.id)
                queue.add(n)
            }
        }
    }
    val leaves = mutableMapOf<String, Int>()
    fun leafCount(id: String): Int = leaves.getOrPut(id) {
        val ch = children[id].orEmpty()
        if (ch.isEmpty()) 1 else ch.sumOf { leafCount(it) }
    }
    val autoAngle = mutableMapOf<String, Double>()
    fun assign(id: String, a0: Double, a1: Double) {
        autoAngle[id] = (a0 + a1) / 2.0
        val ch = children[id].orEmpty()
        if (ch.isEmpty()) return
        val total = ch.sumOf { leafCount(it) }.toDouble()
        var cursor = a0
        for (c in ch) {
            val span = (a1 - a0) * leafCount(c) / total
            assign(c, cursor, cursor + span)
            cursor += span
        }
    }
    assign(center.id, 0.0, 360.0)
    return queue.map { p ->
        val level = if (p.is_central) 0 else if (p.map_level > 0) p.map_level else dist.getValue(p.id)
        val angle = if (p.map_angle >= 0.0 && !p.is_central) p.map_angle else (autoAngle[p.id] ?: 0.0)
        MapNode(p, level, angle)
    }
}

@Composable
private fun PlansMapWorkspace(vm: LifeViewModel) {
    val plans = vm.data.detailed_plans
    var openId by remember { mutableStateOf("") }
    val center = plans.firstOrNull { it.is_central }
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("КАРТА ПЛАНОВ", color = Green, fontFamily = blenderBold(), fontSize = 22.sp)
                Text("Центр — главный план, ветки — связанные планы", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
            }
            if (center != null) GreenButton("СБРОС") { vm.resetPlanMapPlacement() }
        }
        when {
            plans.isEmpty() -> EmptyFeatureCard("Карта планов пуста", "Создайте план и сделайте его центральным, затем свяжите с ним другие планы.") { vm.activeTab = "ПЛАНЫ В ПОДРОБНОСТЯХ" }
            center == null -> Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Выберите центральный план", color = Light, fontFamily = blenderBold(), fontSize = 14.sp)
                    plans.forEach { p -> GreenButton(p.title.ifBlank { "Без названия" }) { vm.setCentralPlan(p.id, true) } }
                }
            }
            else -> PlansMapCanvas(vm, plans) { openId = it }
        }
    }
    plans.firstOrNull { it.id == openId }?.let { PlanCardDialog(vm, it) { openId = "" } }
}

@Composable
private fun ColumnScope.PlansMapCanvas(vm: LifeViewModel, plans: List<DetailedPlanItem>, onOpen: (String) -> Unit) {
    val nodes = remember(plans) { computePlanMap(plans) }
    val unlinked = plans.filter { p -> nodes.none { it.plan.id == p.id } }
    val maxLevel = (nodes.maxOfOrNull { it.level } ?: 0).coerceAtLeast(1)
    val stepDp = 118.dp
    val radiusDp = stepDp * maxLevel + 64.dp
    val sizeDp = radiusDp * 2
    val density = LocalDensity.current
    val stepPx = with(density) { stepDp.toPx() }
    val sizePx = with(density) { sizeDp.toPx() }
    val c = Offset(sizePx / 2f, sizePx / 2f)
    var dragId by remember { mutableStateOf("") }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val hs = rememberScrollState()
    val vs = rememberScrollState()
    fun polar(level: Int, deg: Double): Offset {
        val a = (deg - 90.0) * kotlin.math.PI / 180.0
        return Offset(c.x + (kotlin.math.cos(a) * stepPx * level).toFloat(), c.y + (kotlin.math.sin(a) * stepPx * level).toFloat())
    }
    val base = nodes.associate { it.plan.id to polar(it.level, it.angleDeg) }
    val shown = base.mapValues { (id, p) -> if (id == dragId) p + dragOffset else p }
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xF2101412)).border(1.dp, Grid, RoundedCornerShape(10.dp))) {
        val viewW = with(density) { maxWidth.toPx() }
        val viewH = with(density) { maxHeight.toPx() }
        LaunchedEffect(sizePx) {
            hs.scrollTo(((sizePx - viewW) / 2f).coerceAtLeast(0f).toInt())
            vs.scrollTo(((sizePx - viewH) / 2f).coerceAtLeast(0f).toInt())
        }
        Box(Modifier.fillMaxSize().horizontalScroll(hs).verticalScroll(vs)) {
            Box(Modifier.size(sizeDp)) {
                Canvas(Modifier.fillMaxSize()) {
                    for (k in 1..maxLevel) {
                        drawCircle(Grid.copy(alpha = 0.7f), radius = stepPx * k, center = c, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))))
                    }
                    val drawn = mutableSetOf<String>()
                    nodes.forEach { n ->
                        n.plan.linked_plan_ids.forEach { other ->
                            val a = shown[n.plan.id]; val b = shown[other]
                            val key = listOf(n.plan.id, other).sorted().joinToString("|")
                            if (a != null && b != null && drawn.add(key)) { val touchesCenter = n.level == 0 || nodes.firstOrNull { it.plan.id == other }?.level == 0; drawLine(if (touchesCenter) Green.copy(alpha = 0.9f) else Color(0xFF7A7A7A).copy(alpha = 0.75f), a, b, strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round) }
                        }
                    }
                }
                for (k in 1..maxLevel) {
                    Text("УР. $k", color = Light.copy(alpha = 0.7f), fontFamily = blenderBold(), fontSize = 8.sp,
                        modifier = Modifier.offset { IntOffset((c.x - 16.dp.toPx()).roundToInt(), (c.y - stepPx * k - 12.dp.toPx()).roundToInt()) })
                }
                nodes.forEach { n ->
                    val isCenter = n.level == 0
                    val w = 104.dp
                    val h = 52.dp
                    val wPx = with(density) { w.toPx() }; val hPx = with(density) { h.toPx() }
                    val p = shown.getValue(n.plan.id)
                    val dragging = dragId == n.plan.id
                    val fade = if (isCenter) 1f else when (n.level) { 1 -> 1f; 2 -> 0.8f; else -> 0.62f }
                    Column(
                        Modifier.offset { IntOffset((p.x - wPx / 2f).roundToInt(), (p.y - 13.dp.toPx()).roundToInt()) }
                            .width(w).height(h)
                            .pointerInput(n.plan.id) { detectTapGestures(onTap = { onOpen(n.plan.id) }) }
                            .then(
                                if (isCenter) Modifier else Modifier.pointerInput(n.plan.id, n.level, n.angleDeg) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragId = n.plan.id; dragOffset = Offset.Zero },
                                        onDrag = { change, amount -> change.consume(); dragOffset += amount },
                                        onDragEnd = {
                                            val fin = (base[n.plan.id] ?: c) + dragOffset
                                            val rel = fin - c
                                            val dist = kotlin.math.hypot(rel.x.toDouble(), rel.y.toDouble())
                                            val lvl = kotlin.math.round(dist / stepPx).toInt().coerceIn(1, maxLevel + 1)
                                            var deg = kotlin.math.atan2(rel.y.toDouble(), rel.x.toDouble()) * 180.0 / kotlin.math.PI + 90.0
                                            deg = ((deg % 360.0) + 360.0) % 360.0
                                            vm.setPlanMapPlacement(n.plan.id, lvl, deg)
                                            dragId = ""; dragOffset = Offset.Zero
                                        },
                                        onDragCancel = { dragId = ""; dragOffset = Offset.Zero }
                                    )
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(if (isCenter) 16.dp else if (dragging) 13.dp else 10.dp).clip(CircleShape).background(if (isCenter || dragging) Green else Color(0xFFC8C8C8).copy(alpha = fade)))
                        Spacer(Modifier.height(3.dp))
                        Text(n.plan.title.ifBlank { "Без названия" }, color = White.copy(alpha = fade), fontFamily = if (isCenter) blenderBold() else blenderBook(), fontSize = if (isCenter) 12.sp else 10.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp)
                    }
                }
            }
        }
    }
    Text("Нажмите на план — откроется карточка. Зажмите план и перетащите, чтобы переместить его на другой уровень.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
    if (unlinked.isNotEmpty()) {
        Text("Не связаны с центром (свяжите в «Планы в подробностях»):", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            unlinked.forEach { u ->
                Surface(Modifier.clickable { onOpen(u.id) }, color = Color(0xFF111713), border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(6.dp)) {
                    Text(u.title.ifBlank { "Без названия" }, color = Color.LightGray, fontFamily = blenderBook(), fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanCardDialog(vm: LifeViewModel, plan: DetailedPlanItem, onClose: () -> Unit) {
    val linked = vm.data.detailed_plans.filter { plan.linked_plan_ids.contains(it.id) || it.linked_plan_ids.contains(plan.id) }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Color(0xFF050B10),
        shape = RoundedCornerShape(16.dp),
        title = { Text(plan.title.ifBlank { "Без названия" }, color = Green, fontFamily = blenderBold(), fontSize = 18.sp) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plan.is_central) Text("Центральный план карты", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                Text(plan.description.ifBlank { "Описание пока не добавлено." }, color = White, fontFamily = blenderBook(), fontSize = 12.sp, lineHeight = 18.sp)
                if (plan.subitems.isNotEmpty()) {
                    Text("ПОДПУНКТЫ", color = Green, fontFamily = blenderBold(), fontSize = 11.sp)
                    plan.subitems.forEachIndexed { i, s ->
                        Text((if (s.numbered) "${i + 1}. " else "• ") + s.text, color = if (s.struck) Color.Gray else White, fontFamily = blenderBook(), fontSize = 12.sp,
                            textDecoration = if (s.struck) TextDecoration.LineThrough else null,
                            modifier = Modifier.background(if (s.highlighted) parseUiColor(s.highlight_color).copy(alpha = 0.3f) else Color.Transparent))
                    }
                }
                if (linked.isNotEmpty()) {
                    Text("СВЯЗАННЫЕ ПЛАНЫ", color = Green, fontFamily = blenderBold(), fontSize = 11.sp)
                    Text(linked.joinToString(", ") { it.title.ifBlank { "Без названия" } }, color = Light, fontFamily = blenderBook(), fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.selectedPlanId = plan.id; vm.activeTab = "ПЛАНЫ В ПОДРОБНОСТЯХ"; onClose() }) { Text("Редактировать", color = Green, fontFamily = blenderBold()) } },
        dismissButton = { TextButton(onClick = onClose) { Text("Закрыть", color = Color.Gray) } }
    )
}


@Composable
private fun PlanCreateDialog(onDismiss: () -> Unit, onCreate: (String, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var central by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF050B10),
        shape = RoundedCornerShape(16.dp),
        title = { Text("НОВЫЙ ПЛАН", color = Green, fontFamily = blenderBold(), fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EditorField("Название плана", title) { title = it.take(60) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { central = !central }) {
                    Checkbox(checked = central, onCheckedChange = { central = it }, colors = CheckboxDefaults.colors(checkedColor = Green))
                    Text("Сделать центральным (карта планов)", color = White, fontFamily = blenderBook(), fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(title, central) }) { Text("Создать", color = Green, fontFamily = blenderBold()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Red) } }
    )
}

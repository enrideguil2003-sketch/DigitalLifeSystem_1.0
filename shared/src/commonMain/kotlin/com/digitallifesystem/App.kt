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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
        MainScreen(vm)
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
        custom_trainings = custom_trainings.map { it.copy() }.toMutableList(),
        training_segment_overrides = training_segment_overrides.toMutableMap(),
        training_plan_by_date = training_plan_by_date.toMutableMap(),
        training_last_activity_by_group = training_last_activity_by_group.toMutableMap(),
        training_penalty_segments = training_penalty_segments.toMutableMap(),
        notes = notes.map { it.copy(linked_note_ids = it.linked_note_ids.toMutableList()) }.toMutableList(),
        detailed_plans = detailed_plans.map { plan -> plan.copy(subitems = plan.subitems.map { it.copy() }.toMutableList()) }.toMutableList(),
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
            users["Reg1nald"] = UserData(password = "Qpzmal01", role = "Системный администратор", display_name = "Reg1nald")
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
    fun createDetailedPlan() {
        val plan = DetailedPlanItem(id = recordId("plan", data.detailed_plans.size), title = "Новый план")
        data = data.copy(detailed_plans = (data.detailed_plans + plan).toMutableList())
        selectedPlanId = plan.id
        activeTab = "ПЛАНЫ В ПОДРОБНОСТЯХ"
        save()
    }
    fun updateDetailedPlan(updated: DetailedPlanItem) {
        data = data.copy(detailed_plans = data.detailed_plans.map { current ->
            if (current.id == updated.id) updated.copy(subitems = updated.subitems.map { it.copy() }.toMutableList(), updated_at = todayIso()) else current
        }.toMutableList())
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
        data = data.copy(detailed_plans = data.detailed_plans.filterNot { it.id == id }.toMutableList())
        selectedPlanId = data.detailed_plans.firstOrNull()?.id.orEmpty()
        save()
    }
    fun addFoodEntry(name: String, meal: String, calories: Int, protein: Double, fat: Double, carbs: Double) {
        val entry = FoodEntry(id = recordId("food", data.food_entries.size), date = nutritionDate, meal = meal, name = name.ifBlank { "Продукт" }, calories = calories.coerceAtLeast(0), protein_grams = protein.coerceAtLeast(0.0), fat_grams = fat.coerceAtLeast(0.0), carbs_grams = carbs.coerceAtLeast(0.0))
        data = data.copy(food_entries = (data.food_entries + entry).toMutableList())
        save()
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

    fun canExecuteTraining(date:String):Boolean = isAdmin || date <= appTodayIso()

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

        val base=if(group=="Выносливость") 3 else 4
        val custom=customTrainingExercises(group,date)
        val doneGenerated=(0 until base).count{isTrainingExerciseDone(date,group,it)}
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
        val base=if(group=="Выносливость") 3 else 4
        return base+customTrainingExercises(group,date).size
    }

    fun completedTrainingDays(group:String):Int {
        ensureTrainingCache()
        completedDaysCache[group]?.let{return it}
        val dates=(data.training_completions.mapNotNull{it.substringBefore('|').takeIf{it.isNotBlank()}} + data.custom_trainings.filter{it.muscle_group==group}.map{it.date}).distinct()
        var count=0
        dates.forEach{d->
            if(trainingGroupFor(d)==group){
                val base=if(group=="Выносливость") 3 else 4
                val custom=customTrainingExercises(group,d)
                val generatedAll=(0 until base).all{isTrainingExerciseDone(d,group,it)}
                val customAll=custom.all{it.done}
                if(generatedAll && customAll && (base+custom.size)>0) count++
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
            val base = if (group == "Выносливость" && LocalDate.parse(date).dayOfWeek.ordinal == 5) 3 else 4
            val custom = customTrainingExercises(group, date)
            val generatedDone = data.training_completions.count { it.startsWith("$date|$group|") }
            val customDone = custom.count { it.done }
            val total = base + custom.size
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

    fun trainingPlan(group:String,date:String):List<TrainingExercise> {
        val level=muscleLevel(group)
        val tier=tierForLevel(level)
        val difficulty=trainingDifficultyForLevel(level)
        val weekday=LocalDate.parse(date).dayOfWeek.ordinal
        return (when (group) {
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
            "Ноги" -> if (weekday == 4) when(tier){
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
            } else emptyList()
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
        }).mapIndexed { index, ex ->
            ex.copy(
                prescription=weeklyProgressedPrescription(ex.name,ex.prescription,trainingProgressWeek(group)),
                description=trainingDescriptionFor(ex.name),
                key=trainingKey(date,group,index),
                isCustom=false
            )
        } + customTrainingExercises(group,date).map {
            TrainingExercise(
                name=it.name,
                prescription="Пользовательская тренировка",
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
        if(group.isBlank() || (!isAdmin && date > appTodayIso())) return
        trainingCreatorGroup=group
        trainingCreatorDate=date
        trainingCreatorOpen=true
    }
    fun closeTrainingCreator(){trainingCreatorOpen=false}
    fun addCustomTraining(group:String,date:String,name:String,description:String){
        if(group.isBlank() || name.isBlank()) return
        if(!isAdmin && date > appTodayIso()) return
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

@Composable private fun MainScreen(vm:LifeViewModel){
    LaunchedEffect(Unit){while(true){vm.tick();delay(30.seconds)}}
    BoxWithConstraints(Modifier.fillMaxSize().background(Black)){
        val isPhone = this.maxWidth < 700.dp
        UserBackground(vm)
        if (vm.settingsOpen) {
            SettingsDialog(vm)
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
            .background(Color(0xE6050B10))
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
                .clickable { vm.menuOpen = false }
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
                .clickable { },
            color = Color(0xF2050B10),
            shape = drawerShape
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxHeight().width(3.dp).background(Green))
                Column(
                    Modifier.fillMaxHeight().weight(1f).padding(start=16.dp, end=16.dp, top=notchHeight + 8.dp, bottom=14.dp)
                ) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        DrawerItem("ЗАДАЧИ", vm.activeTab == "ЗАДАЧИ", large = false) { selectDrawerTab(vm, "ЗАДАЧИ") }
                        DrawerItem("СПИСОК ЗАДАЧ", vm.activeTab == "ЗАДАЧИ", large = true) { selectDrawerTab(vm, "ЗАДАЧИ") }
                        DrawerItem("ЗАМЕТКИ", vm.activeTab == "ЗАМЕТКИ", large = true) { selectDrawerTab(vm, "ЗАМЕТКИ") }
                        DrawerItem("ГРАФЫ", vm.activeTab == "ГРАФЫ", large = true) { selectDrawerTab(vm, "ГРАФЫ") }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF245B25), thickness = 1.dp)
                        DrawerItem("ПЛАНЫ", vm.activeTab.startsWith("ПЛАН"), large = false) { selectDrawerTab(vm, "ПЛАНЫ") }
                        DrawerItem("КАРТА ПЛАНОВ", vm.activeTab == "КАРТА ПЛАНОВ", large = true) { selectDrawerTab(vm, "КАРТА ПЛАНОВ") }
                        DrawerItem("ПЛАНЫ В ПОДРОБНОСТЯХ", vm.activeTab == "ПЛАНЫ В ПОДРОБНОСТЯХ", large = true) { selectDrawerTab(vm, "ПЛАНЫ В ПОДРОБНОСТЯХ") }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF245B25), thickness = 1.dp)
                        DrawerItem("ЗДОРОВЬЕ", vm.activeTab == "ЗДОРОВЬЕ", large = false) { selectDrawerTab(vm, "ЗДОРОВЬЕ") }
                        DrawerItem("ТРЕНИРОВКИ", vm.healthTab == "ТРЕНИРОВКИ" && vm.activeTab == "ЗДОРОВЬЕ", large = true) { selectDrawerTab(vm, "ТРЕНИРОВКИ") }
                        DrawerItem("ПИТАНИЕ", vm.healthTab == "ПИТАНИЕ" && vm.activeTab == "ЗДОРОВЬЕ", large = true) { selectDrawerTab(vm, "ПИТАНИЕ") }
                        DrawerItem("СОН", vm.healthTab == "СОН" && vm.activeTab == "ЗДОРОВЬЕ", large = true) { selectDrawerTab(vm, "СОН") }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF245B25), thickness = 1.dp)
                        DrawerItem("ФИНАНСЫ", vm.activeTab == "ФИН. РАСЧЕТ" || vm.activeTab == "ГРАФИКИ", large = false) { selectDrawerTab(vm, "ФИН. РАСЧЕТ") }
                        DrawerItem("ФИН. РАСЧЕТ", vm.activeTab == "ФИН. РАСЧЕТ", large = true) { selectDrawerTab(vm, "ФИН. РАСЧЕТ") }
                        DrawerItem("ГРАФИКИ", vm.activeTab == "ГРАФИКИ", large = true) { selectDrawerTab(vm, "ГРАФИКИ") }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF245B25), thickness = 1.dp)
                        DrawerItem("СТАТИСТИКА", vm.activeTab == "СТАТИСТИКА", large = false) { selectDrawerTab(vm, "СТАТИСТИКА") }
                        DrawerItem("ДОСТИЖЕНИЯ", vm.activeTab == "ДОСТИЖЕНИЯ", large = true) { selectDrawerTab(vm, "ДОСТИЖЕНИЯ") }
                        DrawerItem("НАСТРОЙКИ", vm.settingsOpen, large = false) { vm.menuOpen = false; vm.settingsOpen = true }
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

@Composable
private fun DrawerItem(label: String, selected: Boolean, large: Boolean, onClick: () -> Unit) {
    val itemShape = RoundedCornerShape(5.dp)
    Row(
        Modifier.fillMaxWidth()
            .padding(bottom = 3.dp)
            .clip(itemShape)
            .background(if (selected) Color(0xFF0B1B0B) else Color.Transparent)
            .clickable(onClick = onClick)
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
                Text("Фото сохраняется в профиль и остаётся после перезапуска приложения.", color = Color(0xFFB8DFF0), fontFamily = blenderBook(), fontSize = 12.sp)
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
            Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-1).dp).clip(CircleShape).background(Green).size(15.dp),contentAlignment=Alignment.Center){
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
        "ГРАФЫ" -> { NotesGraphWorkspace(vm); return }
        "ПЛАНЫ В ПОДРОБНОСТЯХ", "ПЛАНЫ" -> { DetailedPlansWorkspace(vm); return }
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
        HealthSubTabs(vm)
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
                    DropdownMenu(expanded=planMenuOpen, onDismissRequest={planMenuOpen=false}) {
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
                Text("Накоплено: ${vm.trainingCumulativePercent(group)}% • Сегодня: ${vm.trainingDayPercent(group,selectedDate)}%",color=Color(0xFFB8DFF0),fontFamily=blenderBook(),fontSize=daySize,maxLines=2,softWrap=true)
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
                if(locked) Text("БУДУЩАЯ",color=Color(0xFFFFA0A0),fontFamily=blenderBold(),fontSize=9.sp)
            }
            if(group.isBlank()) {
                Text("Восстановление",color=Color.Gray,fontFamily=blenderBook(),fontSize=12.sp)
            } else {
                plan.forEach { exercise ->
                    val done=vm.isTrainingExerciseDone(exercise)
                    val exerciseLocked=locked && !vm.isAdmin
                    Surface(Modifier.fillMaxWidth(),color=if(done)Color(0xFF082514) else Color(0xFF071007),border=BorderStroke(1.dp,if(done)Green else Grid),shape=RoundedCornerShape(8.dp)){
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
                if(locked) Text("Для пользователя тренировка станет доступна в день по плану.",color=Color(0xFFFFA0A0),fontFamily=blenderBook(),fontSize=9.sp)
                if(plan.isNotEmpty()) Text("Выполнено: ${(vm.trainingDayProgress(group,date)*100).toInt()}%",color=Light,fontFamily=blenderBold(),fontSize=12.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
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
            Text("НАЗВАНИЕ ЗАМЕТКИ", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
            OutlinedTextField(
                value = note.title,
                onValueChange = { vm.updateNote(note.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = White, fontSize = 21.sp, fontFamily = blenderBold()),
                colors = darkFields(), placeholder = { Text("Название заметки", color = Color.Gray) }
            )
            Text("ТЕМА ЗАМЕТКИ", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
            OutlinedTextField(
                value = note.topic,
                onValueChange = { vm.updateNote(note.copy(topic = it)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = Color(0xFFB0D9B0), fontSize = 14.sp, fontFamily = blenderBook()),
                colors = darkFields(), placeholder = { Text("Например: Изучение JavaScript", color = Color.Gray) }
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
private fun NotesGraphWorkspace(vm: LifeViewModel) {
    val notes = vm.data.notes
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ГРАФЫ ЗАМЕТОК", color = Green, fontFamily = blenderBold(), fontSize = 23.sp)
                Text("Соединяйте заметки, чтобы видеть связи между идеями", color = Color.Gray, fontFamily = blenderBook(), fontSize = 11.sp)
            }
            GreenButton("+ ЗАМЕТКА") { vm.createNote() }
        }
        if (notes.isEmpty()) {
            EmptyFeatureCard("Граф пока пуст", "Создайте две или больше заметок и свяжите их между собой.") { vm.activeTab = "ЗАМЕТКИ" }
        } else {
            val graphHeight = maxOf(260.dp, (notes.size * 140 + 45).dp)
            BoxWithConstraints(Modifier.fillMaxWidth().height(graphHeight)) {
                val oneColumn = maxWidth < 520.dp
                val fractions = notes.indices.map { i -> if (oneColumn) 0.5f else when (i % 3) { 0 -> 0.20f; 1 -> 0.50f; else -> 0.80f } }
                val rows = notes.indices.map { i -> if (oneColumn) i else i / 3 }
                Canvas(Modifier.fillMaxSize()) {
                    notes.forEachIndexed { fromIndex, from ->
                        from.linked_note_ids.forEach { targetId ->
                            val toIndex = notes.indexOfFirst { it.id == targetId }
                            if (toIndex >= 0 && toIndex != fromIndex) {
                                val start = androidx.compose.ui.geometry.Offset(size.width * fractions[fromIndex], (62 + rows[fromIndex] * 140).dp.toPx())
                                val end = androidx.compose.ui.geometry.Offset(size.width * fractions[toIndex], (62 + rows[toIndex] * 140).dp.toPx())
                                drawLine(color = Color(0xFF4DCB73).copy(alpha = 0.82f), start = start, end = end, strokeWidth = 2.dp.toPx())
                            }
                        }
                    }
                }
                notes.forEachIndexed { index, note ->
                    Surface(
                        Modifier.offset(x = maxWidth * fractions[index] - 74.dp, y = (32 + rows[index] * 140).dp).width(148.dp).height(62.dp).clickable { vm.selectedNoteId = note.id; vm.activeTab = "ЗАМЕТКИ" },
                        color = Color(0xF20B1410), border = BorderStroke(1.5.dp, if (note.linked_note_ids.isEmpty()) Grid else Green), shape = RoundedCornerShape(9.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.Center) {
                            Text(note.title.ifBlank { "Без названия" }, color = White, fontFamily = blenderBold(), fontSize = 12.sp, maxLines = 2)
                            Text(note.topic.ifBlank { "Без темы" }, color = Light, fontFamily = blenderBook(), fontSize = 9.sp, maxLines = 1)
                            Text("${note.linked_note_ids.size} связей", color = Color.Gray, fontFamily = blenderBook(), fontSize = 8.sp)
                        }
                    }
                }
            }
            val linkedCount = notes.flatMap { from -> from.linked_note_ids.map { target -> listOf(from.id, target).sorted().joinToString("|") } }.distinct().size
            Text("Заметок: ${notes.size} • связей: $linkedCount", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
        }
    }
}

@Composable
private fun DetailedPlansWorkspace(vm: LifeViewModel) {
    val plans = vm.data.detailed_plans
    val selected = plans.firstOrNull { it.id == vm.selectedPlanId } ?: plans.firstOrNull()
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ПЛАНЫ В ПОДРОБНОСТЯХ", color = Green, fontFamily = blenderBold(), fontSize = 22.sp)
                Text("Большие описания, подпункты и форматирование", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
            }
            GreenButton("+ СОЗДАТЬ ПЛАН") { vm.createDetailedPlan() }
        }
        if (plans.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                plans.forEach { plan ->
                    Surface(Modifier.clickable { vm.selectedPlanId = plan.id }, color = if (selected?.id == plan.id) Color(0xFF18301C) else Panel, border = BorderStroke(1.dp, if (selected?.id == plan.id) Green else Grid), shape = RoundedCornerShape(7.dp)) {
                        Text(plan.title.ifBlank { "Без названия" }, color = White, fontFamily = blenderBook(), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), maxLines = 1)
                    }
                }
            }
        }
        if (selected == null) {
            EmptyFeatureCard("Подробные планы", "Создайте план, добавьте длинное описание и разбейте его на подпункты.") { vm.createDetailedPlan() }
        } else {
            PlanEditorPane(vm, selected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlanEditorPane(vm: LifeViewModel, plan: DetailedPlanItem, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("НАЗВАНИЕ ПЛАНА", color = Light, fontFamily = blenderBold(), fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { vm.removeDetailedPlan(plan.id) }) { Text("УДАЛИТЬ", color = Red, fontFamily = blenderBold(), fontSize = 10.sp) }
            }
            OutlinedTextField(value = plan.title, onValueChange = { vm.updateDetailedPlan(plan.copy(title = it)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = White, fontSize = 20.sp, fontFamily = blenderBold()), colors = darkFields(), placeholder = { Text("Название плана", color = Color.Gray) })
            Text("ПОДРОБНОЕ ОПИСАНИЕ", color = Light, fontFamily = blenderBold(), fontSize = 10.sp)
            OutlinedTextField(value = plan.description, onValueChange = { vm.updateDetailedPlan(plan.copy(description = it)) }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp), minLines = 5, maxLines = 15, textStyle = TextStyle(color = White, fontSize = 12.sp, fontFamily = blenderBook(), lineHeight = 18.sp), colors = darkFields(), placeholder = { Text("Опишите этапы, критерии и необходимые материалы…", color = Color.Gray) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ПОДПУНКТЫ", color = Green, fontFamily = blenderBold(), fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                GreenButton("+ ПОДПУНКТ") { vm.addPlanSubItem(plan.id) }
            }
            if (plan.subitems.isEmpty()) Text("Подпункты можно выделять, нумеровать, зачеркивать и окрашивать.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
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
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ДОБАВИТЬ ПРОДУКТ", color = Green, fontFamily = blenderBold(), fontSize = 16.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Завтрак", "Обед", "Ужин", "Перекус").forEach { option -> MiniToggle(option, meal == option) { meal = option } }
                }
                OutlinedTextField(value = foodName, onValueChange = { foodName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название продукта") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = caloriesText, onValueChange = { caloriesText = it.filter { ch -> ch.isDigit() } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Ккал") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                    OutlinedTextField(value = proteinText, onValueChange = { proteinText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Белки, г") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = fatText, onValueChange = { fatText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Жиры, г") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                    OutlinedTextField(value = carbsText, onValueChange = { carbsText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Углеводы, г") }, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                }
                if (error.isNotBlank()) Text(error, color = Red, fontFamily = blenderBook(), fontSize = 10.sp)
                GreenButton("+ ДОБАВИТЬ В ДНЕВНИК") {
                    val kcal = caloriesText.toIntOrNull()
                    if (foodName.isBlank() || kcal == null) error = "Укажи название и калории продукта." else {
                        vm.addFoodEntry(foodName.trim(), meal, kcal, (proteinText.replace(',', '.').toDoubleOrNull() ?: 0.0), (fatText.replace(',', '.').toDoubleOrNull() ?: 0.0), (carbsText.replace(',', '.').toDoubleOrNull() ?: 0.0))
                        foodName = ""; caloriesText = ""; proteinText = ""; fatText = ""; carbsText = ""; error = ""
                    }
                }
            }
        }
        listOf("Завтрак", "Обед", "Ужин", "Перекус").forEach { mealName ->
            val mealEntries = entries.filter { it.meal == mealName }
            Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(mealName.uppercase(), color = Green, fontFamily = blenderBold(), fontSize = 15.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${mealEntries.sumOf { it.calories }} ккал", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                    }
                    if (mealEntries.isEmpty()) Text("Добавьте блюдо или продукт", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                    mealEntries.forEach { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF182A1B)), contentAlignment = Alignment.Center) { Text("•", color = Green, fontSize = 18.sp) }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, color = White, fontFamily = blenderBook(), fontSize = 12.sp)
                                Text("Б ${entry.protein_grams} г • Ж ${entry.fat_grams} г • У ${entry.carbs_grams} г", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp)
                            }
                            Text("${entry.calories} ккал", color = Light, fontFamily = blenderBold(), fontSize = 11.sp)
                            Text("×", color = Red, modifier = Modifier.clickable { vm.deleteFoodEntry(entry.id) }.padding(start = 9.dp))
                        }
                        HorizontalDivider(color = Color(0xFF18301E), thickness = 1.dp)
                    }
                }
            }
        }
    }
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
    var bedtime by remember(date, stored?.bedtime) { mutableStateOf(stored?.bedtime ?: "23:00") }
    var wakeTime by remember(date, stored?.wake_time) { mutableStateOf(stored?.wake_time ?: "07:00") }
    var quality by remember(date, stored?.quality) { mutableFloatStateOf((stored?.quality ?: 80).toFloat()) }
    var snoring by remember(date, stored?.snoring_minutes) { mutableStateOf((stored?.snoring_minutes ?: 0).toString()) }
    var notes by remember(date, stored?.notes) { mutableStateOf(stored?.notes.orEmpty()) }
    var savedLabel by remember { mutableStateOf("") }
    val minutes = durationMinutes(bedtime, wakeTime)
    val recent = vm.data.sleep_records.sortedBy { it.date }.takeLast(7)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("МОЙ РЕЖИМ", color = Green, fontFamily = blenderBold(), fontSize = 16.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bedtime, onValueChange = { bedtime = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Лёг спать") }, textStyle = TextStyle(color = White, fontSize = 13.sp), colors = darkFields())
                    OutlinedTextField(value = wakeTime, onValueChange = { wakeTime = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Проснулся") }, textStyle = TextStyle(color = White, fontSize = 13.sp), colors = darkFields())
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Время сна", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                        Text("${minutes / 60} ч ${minutes % 60} мин", color = White, fontFamily = blenderBold(), fontSize = 23.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Храп, минуты", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                        OutlinedTextField(value = snoring, onValueChange = { snoring = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = White, fontSize = 12.sp), colors = darkFields())
                    }
                }
                Text("ОЦЕНКА КАЧЕСТВА СНА • ${quality.toInt()}%", color = Light, fontFamily = blenderBold(), fontSize = 10.sp)
                Slider(value = quality, onValueChange = { quality = it }, valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = Green, activeTrackColor = Green))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 2, maxLines = 4, label = { Text("Заметки о сне") }, textStyle = TextStyle(color = White, fontSize = 11.sp), colors = darkFields())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (savedLabel.isNotBlank()) Text(savedLabel, color = Light, fontFamily = blenderBook(), fontSize = 10.sp, modifier = Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                    GreenButton("СОХРАНИТЬ СОН") {
                        if (durationMinutes(bedtime, wakeTime) > 0) {
                            vm.saveSleepRecord(SleepRecord(date = date, bedtime = bedtime, wake_time = wakeTime, quality = quality.toInt(), snoring_minutes = snoring.toIntOrNull() ?: 0, notes = notes))
                            savedLabel = "Запись сохранена"
                        } else savedLabel = "Проверьте время в формате ЧЧ:ММ"
                    }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), color = Panel, border = BorderStroke(1.dp, Grid), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("КАЧЕСТВО СНА • ПОСЛЕДНИЕ ЗАПИСИ", color = Green, fontFamily = blenderBold(), fontSize = 15.sp)
                if (recent.isEmpty()) Text("График появится после сохранения первой записи.", color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp)
                Canvas(Modifier.fillMaxWidth().height(135.dp)) {
                    val chartLeft = 8.dp.toPx(); val chartRight = size.width - 8.dp.toPx(); val chartTop = 10.dp.toPx(); val chartBottom = size.height - 12.dp.toPx()
                    for (i in 0..4) {
                        val y = chartTop + (chartBottom - chartTop) * i / 4f
                        drawLine(Color(0xFF25213D), androidx.compose.ui.geometry.Offset(chartLeft, y), androidx.compose.ui.geometry.Offset(chartRight, y), strokeWidth = 1.dp.toPx())
                    }
                    if (recent.isNotEmpty()) {
                        val points = recent.mapIndexed { index, record ->
                            val x = if (recent.size == 1) (chartLeft + chartRight) / 2f else chartLeft + (chartRight - chartLeft) * index / (recent.size - 1).toFloat()
                            val y = chartBottom - (chartBottom - chartTop) * record.quality.coerceIn(0, 100) / 100f
                            androidx.compose.ui.geometry.Offset(x, y)
                        }
                        val path = Path().apply { points.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) } }
                        drawPath(path, Color(0xFFAD8AFF), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        points.forEach { drawCircle(Color(0xFFE0D2FF), 3.5.dp.toPx(), it) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("КАЧЕСТВО", color = Color(0xFFAD8AFF), fontFamily = blenderBook(), fontSize = 9.sp); Text("0–100%", color = Color.Gray, fontFamily = blenderBook(), fontSize = 9.sp) }
                if (recent.isNotEmpty()) recent.forEach { rec ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(rec.date, color = Color.Gray, fontFamily = blenderBook(), fontSize = 10.sp, modifier = Modifier.width(90.dp))
                        Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF211D35))) { Box(Modifier.fillMaxHeight().fillMaxWidth(rec.quality / 100f).background(Color(0xFFAD8AFF))) }
                        Text(" ${rec.quality}%", color = White, fontFamily = blenderBold(), fontSize = 10.sp)
                    }
                }
            }
        }
    }
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
        "ГРАФЫ" -> { NotesGraphWorkspace(vm); return }
        "ПЛАНЫ В ПОДРОБНОСТЯХ", "ПЛАНЫ" -> { DetailedPlansWorkspace(vm); return }
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

@Composable private fun SettingsDialog(vm:LifeViewModel){
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
                "Личные данные"->ProfileSettings(vm)
                "Внешний вид"->AppearanceSettings(vm)
                "Уведомления"->NotificationSettings(vm)
                else->ProfileSettings(vm)
            }
        }},
        confirmButton={TextButton(onClick={vm.settingsOpen=false}){Text("Закрыть",color=Green,fontFamily=blenderBold())}}
    )
}
@Composable private fun ProfileSettings(vm:LifeViewModel){var name by remember{mutableStateOf(vm.data.display_name)};var login by remember{mutableStateOf(vm.username)};var pass by remember{mutableStateOf("")};EditorField("Никнейм",name){name=it};EditorField("Логин",login){login=it};EditorField("Новый пароль",pass){pass=it};Row(verticalAlignment=Alignment.CenterVertically){Text("Аватар",color=White,fontFamily=blenderBook(),modifier=Modifier.weight(1f));GreenButton("Выбрать"){pickImage{vm.setPhoto(it)}}};Spacer(Modifier.height(6.dp));Button(onClick={vm.updateProfile(name,login,pass);vm.settingsOpen=false},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF071507)),border=BorderStroke(1.dp,Green)){Text("Сохранить личные данные",color=White,fontFamily=blenderBold())}}
@Composable private fun AppearanceSettings(vm:LifeViewModel){var bg by remember{mutableFloatStateOf(vm.data.background_opacity.toFloat())};var block by remember{mutableFloatStateOf(vm.data.block_opacity.toFloat())};var blur by remember{mutableFloatStateOf(vm.data.background_blur.toFloat())};Text("Фон",color=Light,fontFamily=blenderBold());Row(verticalAlignment=Alignment.CenterVertically){GreenButton("Выбрать фон"){pickImage{vm.setBackground(it)}};Spacer(Modifier.width(8.dp));Text(if(vm.data.background.isBlank())"Исходный fon.png" else "Пользовательский фон",color=Color.Gray,fontFamily=blenderBook(),fontSize=11.sp)};Text("Прозрачность фона",color=White,fontFamily=blenderBook());Slider(bg,{bg=it;vm.setAppearance(bg.toDouble(),block.toDouble(),blur.toDouble())},valueRange=.1f..1f);Text("Прозрачность блоков",color=White,fontFamily=blenderBook());Slider(block,{block=it;vm.setAppearance(bg.toDouble(),block.toDouble(),blur.toDouble())},valueRange=.1f..1f);Text("Размытие фона",color=White,fontFamily=blenderBook());Slider(blur,{blur=it;vm.setAppearance(bg.toDouble(),block.toDouble(),blur.toDouble())},valueRange=0f..20f)}
@Composable private fun NotificationSettings(vm:LifeViewModel){Row(verticalAlignment=Alignment.CenterVertically){Text("Уведомления",color=White,fontFamily=blenderBook(),modifier=Modifier.weight(1f));Switch(vm.data.notifications_enabled,{vm.setNotifications(it)},colors=SwitchDefaults.colors(checkedThumbColor=Green,checkedTrackColor=DarkBlue))};Text("Проверка времени выполняется автоматически. При приближении задачи появляется уведомление.",color=Color.Gray,fontFamily=blenderBook(),fontSize=11.sp,modifier=Modifier.padding(top=8.dp));Spacer(Modifier.height(10.dp));Text("Непрочитано: ${vm.unread()}",color=Light,fontFamily=blenderBook())}

@Composable private fun ConfirmDialog(d:DialogState,vm:LifeViewModel){AlertDialog(onDismissRequest={vm.dialog=null},containerColor=Color(0xFF050B10),titleContentColor=Green,textContentColor=White,shape=RoundedCornerShape(16.dp),title={Text(d.title,color=Green,fontFamily=blenderBold())},text={Text(d.text,color=White,fontFamily=blenderBook())},confirmButton={TextButton(onClick={d.onYes();vm.dialog=null}){Text("Подтвердить",color=Green)}},dismissButton={TextButton(onClick={vm.dialog=null}){Text("Отмена",color=Red)}})}

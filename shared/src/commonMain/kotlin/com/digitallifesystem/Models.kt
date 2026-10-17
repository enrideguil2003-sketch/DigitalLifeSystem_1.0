package com.digitallifesystem

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Serializable
data class TaskItem(
    var text: String = "Задача",
    var start_time: String = "8:00 AM",
    var date: String = todayIso(),
    var duration_minutes: Int = 30,
    var difficulty: String = "Легкий",
    var points: Int = 10,
    var level_xp: Int = 10,
    var reputation_xp: Int = 0,
    var reputation_rank: Int = 0,
    var done: Boolean = false,
    var penalized: Boolean = false,
    var hard_missed: Boolean = false,
    var remind: Boolean = false,
    var remind_sent: Boolean = false,
    var start_notification_sent: Boolean = false,
    var description: String = "",
    var muscle_group: String = "",
    var instruction: String = "",
    var xp_awarded_amount: Int = 0,
    var created_date: String = "",
    var completed_date: String = "",
    var training_id: String = ""
)

@Serializable
data class Achievement(val id: String = "", val name: String = "", val description: String = "")


@Serializable
data class CustomTrainingExercise(
    var id: String = "",
    var date: String = todayIso(),
    var muscle_group: String = "",
    var name: String = "Тренировка",
    var description: String = "",
    var done: Boolean = false,
    var created_at: String = "",
    var prescription: String = ""
)


@Serializable
data class NoteItem(
    var id: String = "",
    var title: String = "Новая заметка",
    var topic: String = "Без темы",
    var content: String = "",
    var linked_note_ids: MutableList<String> = mutableListOf(),
    var updated_at: String = todayIso()
)

@Serializable
data class PlanSubItem(
    var id: String = "",
    var text: String = "Новый подпункт",
    var numbered: Boolean = false,
    var struck: Boolean = false,
    var highlighted: Boolean = false,
    var highlight_color: String = "#243A25"
)

@Serializable
data class DetailedPlanItem(
    var id: String = "",
    var title: String = "Новый план",
    var description: String = "",
    var subitems: MutableList<PlanSubItem> = mutableListOf(),
    var linked_plan_ids: MutableList<String> = mutableListOf(),
    var updated_at: String = todayIso(),
    var is_central: Boolean = false,
    var map_level: Int = 0,
    var map_angle: Double = -1.0
)

@Serializable
data class Dish(
    var id: String = "",
    var name: String = "Блюдо",
    var meal: String = "Завтрак",
    var calories: Int = 0,
    var protein_grams: Double = 0.0,
    var fat_grams: Double = 0.0,
    var carbs_grams: Double = 0.0,
    var recipe: String = ""
)

@Serializable
data class FoodEntry(
    var id: String = "",
    var date: String = todayIso(),
    var meal: String = "Завтрак",
    var name: String = "Продукт",
    var calories: Int = 0,
    var protein_grams: Double = 0.0,
    var fat_grams: Double = 0.0,
    var carbs_grams: Double = 0.0
)

@Serializable
data class SleepInterval(
    var bedtime: String = "23:00",
    var wake_time: String = "07:00"
)

@Serializable
data class SleepRecord(
    var date: String = todayIso(),
    // Legacy fields are retained so existing users.json files and older app versions remain readable.
    var bedtime: String = "23:00",
    var wake_time: String = "07:00",
    var quality: Int = 80,
    var day_state: Int = 80,
    var snoring_minutes: Int = 0,
    var notes: String = "",
    // Multiple sleep periods in a single calendar day; empty means use the legacy bedtime/wake_time pair.
    var sleep_intervals: MutableList<SleepInterval> = mutableListOf()
)

@Serializable
data class FinanceSavingsPot(
    var name: String = "Копилка",
    var amount: Double = 0.0
)

/** A reusable daily task definition; completion is recorded independently for each calendar date. */
@Serializable
data class DailyTaskItem(
    var id: String = "",
    var text: String = "Ежедневная задача",
    var description: String = "",
    var start_time: String = "08:00",
    var difficulty: String = "Легкий",
    var done_dates: MutableList<String> = mutableListOf()
)

@Serializable
data class FinanceAccount(
    var id: String = "",
    var name: String = "Основной счёт",
    var opening_balance: Double = 0.0,
    var bank_name: String = "Счёт"
)

@Serializable
data class ReceiptItem(
    var name: String = "",
    var amount: Double = 0.0
)

@Serializable
data class ShoppingItem(
    var id: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var store: String = "",
    var purchased: Boolean = false,
    var entry_id: String = ""
)

@Serializable
data class FinanceEntry(
    var id: String = "",
    var date: String = todayIso(),
    var type: String = "Расход",
    var title: String = "Операция",
    var category: String = "Прочее",
    var amount: Double = 0.0,
    var account_id: String = "",
    var items: MutableList<ReceiptItem> = mutableListOf()
)

@Serializable
data class FinanceBudget(
    var id: String = "",
    var category: String = "Прочее",
    var limit_amount: Double = 0.0
)

@Serializable
data class FinanceCredit(
    var id: String = "",
    var name: String = "Кредит",
    var total_amount: Double = 0.0,
    var monthly_payment: Double = 0.0,
    var remaining: Double = 0.0,
    var paid: Double = 0.0,
    var account_id: String = "",
    var created_date: String = todayIso()
)

@Serializable
data class UserData(
    var password: String = "12345",
    var role: String = "Пользователь",
    var display_name: String = "",
    var level: Int = 0,
    var level_xp: Int = 0,
    var reputation_xp: Int = 0,
    var reputation_rank: Int = 0,
    var achievements: MutableList<Achievement> = mutableListOf(),
    var tasks: MutableList<TaskItem> = mutableListOf(),
    var daily_tasks: MutableList<DailyTaskItem> = mutableListOf(),
    // Persisted per user so the daily-task onboarding hint appears only once.
    var daily_tasks_intro_seen: Boolean = false,
    var notifications: MutableList<String> = mutableListOf(),
    var read_notifications: MutableList<String> = mutableListOf(),
    var photo: String = "",
    var background: String = "",
    var block_background: String = "#00000000",
    var notifications_enabled: Boolean = true,
    var background_opacity: Double = 1.0,
    var block_opacity: Double = 1.0,
    var background_blur: Double = 0.0,
    var block_blur: Double = 0.0,
    var calendar_marks: MutableMap<String, String> = mutableMapOf(),
    var training_completions: MutableList<String> = mutableListOf(),
    var custom_trainings: MutableList<CustomTrainingExercise> = mutableListOf(),
    var training_segment_overrides: MutableMap<String, Int> = mutableMapOf(),
    // Added in v86; missing fields in legacy users.json use these defaults.
    var training_plan_by_date: MutableMap<String, String> = mutableMapOf(),
    var training_last_activity_by_group: MutableMap<String, String> = mutableMapOf(),
    var training_penalty_segments: MutableMap<String, Int> = mutableMapOf(),
    // Additive life-management modules; defaults keep old users.json files readable.
    var notes: MutableList<NoteItem> = mutableListOf(),
    var detailed_plans: MutableList<DetailedPlanItem> = mutableListOf(),
    var food_entries: MutableList<FoodEntry> = mutableListOf(),
    var sleep_records: MutableList<SleepRecord> = mutableListOf(),
    var finance_accounts: MutableList<FinanceAccount> = mutableListOf(),
    var finance_entries: MutableList<FinanceEntry> = mutableListOf(),
    // A single named savings pot; null until the user creates it.
    var finance_savings_pot: FinanceSavingsPot? = null,
    var finance_budgets: MutableList<FinanceBudget> = mutableListOf(),
    var nutrition_calorie_target: Int = 2500,
    // Monday of the week when the training program starts. Used only for calendar week numbering.
    var training_start_date: String = todayIso(),
    var app_data_version: Int = 21,
    var training_hidden: MutableList<String> = mutableListOf(),
    var dishes: MutableList<Dish> = mutableListOf(),
    var dishes_seeded: Boolean = false,
    // v2.0.0: credits on the finance screen. Defaults keep old users.json readable.
    var finance_credits: MutableList<FinanceCredit> = mutableListOf(),
    var shopping_items: MutableList<ShoppingItem> = mutableListOf()
)

fun todayIso(): String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

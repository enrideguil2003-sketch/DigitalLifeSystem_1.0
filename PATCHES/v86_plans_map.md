# v1.4.0 — Карта планов
Только карта планов, ничего другого не менялось.
- Models.kt: DetailedPlanItem + is_central, map_level, map_angle (с значениями по умолчанию, данные не теряются).
- App.kt: PlansMapWorkspace (карта), PlanCardDialog (карточка), PlanCreateDialog (создание с галочкой «центральный»),
  чекбокс «Центральный план» в редакторе плана, VM: setCentralPlan / setPlanMapPlacement / resetPlanMapPlacement.
- versionCode 6, versionName 1.4.0.

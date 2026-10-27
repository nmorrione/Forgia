package io.github.nmorrione.forgia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.edit.ExerciseEditor
import io.github.nmorrione.forgia.ui.edit.FoodEditor
import io.github.nmorrione.forgia.ui.edit.FoodsEditor
import io.github.nmorrione.forgia.ui.edit.MealEditor
import io.github.nmorrione.forgia.ui.edit.ProgramEditor
import io.github.nmorrione.forgia.ui.edit.SectionsEditor
import io.github.nmorrione.forgia.ui.edit.SessionEditor
import io.github.nmorrione.forgia.ui.edit.SubstitutionsEditor
import io.github.nmorrione.forgia.ui.edit.TargetsEditor
import io.github.nmorrione.forgia.ui.screens.CalendarScreen
import io.github.nmorrione.forgia.ui.screens.FoodScreen
import io.github.nmorrione.forgia.ui.screens.GuideScreen
import io.github.nmorrione.forgia.ui.screens.PlansScreen
import io.github.nmorrione.forgia.ui.screens.TodayScreen
import io.github.nmorrione.forgia.ui.theme.ForgiaTheme
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.cond

@Composable
fun ForgiaApp(vm: AppViewModel) {
    ForgiaTheme {
        val route = vm.stack.lastOrNull()
        BackHandler(enabled = route != null) { vm.pop() }
        BackHandler(enabled = route == null && vm.tab != Tab.OGGI) { vm.tab = Tab.OGGI }
        Surface(color = Pal.bg, modifier = Modifier.fillMaxSize()) {
            if (route != null) EditorHost(route, vm) else MainTabs(vm)
        }
    }
}

@Composable
private fun EditorHost(route: Route, vm: AppViewModel) {
    when (route) {
        Route.ProgramEdit -> ProgramEditor(vm)
        is Route.SessionEdit -> SessionEditor(vm, route.id)
        is Route.ExerciseEdit -> ExerciseEditor(vm, route.sessionId, route.exerciseId)
        Route.TargetsEdit -> TargetsEditor(vm)
        is Route.MealEdit -> MealEditor(vm, route.day, route.mealId)
        Route.Foods -> FoodsEditor(vm)
        is Route.FoodEdit -> FoodEditor(vm, route.id)
        Route.SubstitutionsEdit -> SubstitutionsEditor(vm)
        is Route.SectionsEdit -> SectionsEditor(vm, route.kind)
    }
}

@Composable
private fun MainTabs(vm: AppViewModel) {
    val p = Pal
    Column(Modifier.fillMaxSize()) {
        // Intestazione
        Box(Modifier.fillMaxWidth().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
            Row(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Forgia", style = cond(22, FontWeight.Bold))
                Text("  ·  " + vm.data.program.name, style = cond(18, FontWeight.Medium), color = p.muted, modifier = Modifier.weight(1f), maxLines = 1)
                LinkBtn("Programma") { vm.push(Route.ProgramEdit) }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (vm.tab) {
                Tab.OGGI -> TodayScreen(vm)
                Tab.CALENDARIO -> CalendarScreen(vm)
                Tab.SCHEDE -> PlansScreen(vm)
                Tab.CIBO -> FoodScreen(vm)
                Tab.GUIDA -> GuideScreen(vm)
            }
        }
        // Barra delle sezioni
        Column(Modifier.fillMaxWidth().background(p.surface).navigationBarsPadding()) {
            HorizontalDivider(color = p.line)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    Tab.entries.forEach { t ->
                        val on = vm.tab == t
                        Box(
                            Modifier.weight(1f).clickable { vm.tab = t }.padding(top = 0.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            if (on) {
                                Box(
                                    Modifier.fillMaxWidth(0.56f).height(3.dp)
                                        .background(p.ink, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
                                )
                            }
                            Text(
                                t.label,
                                style = cond(16),
                                color = if (on) p.ink else p.muted,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 15.dp, bottom = 17.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

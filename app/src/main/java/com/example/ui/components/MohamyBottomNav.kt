package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MohamyBlack
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyGold

data class MohamyBottomNavItem(
  val label: String,
  val icon: ImageVector,
  val selected: Boolean,
  val onClick: () -> Unit,
)

@Composable
fun MohamyBottomNav(
  items: List<MohamyBottomNavItem>,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(30.dp),
      color = MohamyBlack,
      shadowElevation = 18.dp
    ) {
      NavigationBar(
        modifier = Modifier.fillMaxWidth().height(74.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
      ) {
        items.forEach { item ->
          NavigationBarItem(
            selected = item.selected,
            onClick = item.onClick,
            icon = { Icon(item.icon, contentDescription = item.label) },
            label = {
              Text(
                text = item.label,
                fontWeight = FontWeight.Bold
              )
            },
            alwaysShowLabel = true,
            colors =
              NavigationBarItemDefaults.colors(
                selectedIconColor = MohamyGold,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MohamyGold.copy(alpha = 0.14f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
          )
        }
      }
    }
  }
}

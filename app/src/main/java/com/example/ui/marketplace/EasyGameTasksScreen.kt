package com.example.ui.marketplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MicroTask
import com.example.data.model.TaskSubmission
import com.example.ui.home.UiState
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite
import java.util.Locale

@Composable
fun EasyGameTasksScreen(
  tasksState: UiState<List<MicroTask>>,
  submissionsState: UiState<List<TaskSubmission>>,
  isHindi: Boolean,
  onSubmitProof: (String, String, String) -> Unit,
) {
  val allTasks = (tasksState as? UiState.Success)?.data ?: emptyList()
  val gameTasks = allTasks.filter { it.category == "GAME_TASK" }
  val submissions = (submissionsState as? UiState.Success)?.data ?: emptyList()

  var activeGame by remember { mutableStateOf<EasyGameType?>(null) }
  var linkedTaskForGame by remember { mutableStateOf<MicroTask?>(null) }
  var completedProofSummary by remember { mutableStateOf<String?>(null) }
  var gamePassed by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = if (isHindi) "सभी आसान गेम कार्य (All Easy Game Tasks)" else "All Easy Game Tasks",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text =
              if (isHindi) {
                "100% कौशल और QA-आधारित आसान मिनी-गेम। कोई सट्टेबाजी नहीं। एडमिन द्वारा कार्य प्रकाशित व सत्यापित होने पर पारदर्शी इनाम मिलता है।"
              } else {
                "6 casual skill & UX-QA mini-games. No wagering or fake coins — rewards credit only after Admin catalog verification."
              },
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite.copy(alpha = 0.85f),
          )
        }
      }
    }

    items(EasyGameType.entries, key = { it.name }) { game ->
      val matchedTask = gameTasks.firstOrNull { it.id == game.defaultTaskId }
      val sub = matchedTask?.let { t -> submissions.firstOrNull { it.taskId == t.id } }

      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = if (matchedTask != null) "LIVE TASK" else "PRACTICE MODE",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
            if (matchedTask != null) {
              Text(
                text = "Net ₹${String.format(Locale.US, "%.2f", matchedTask.workerNetInr)}",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldAccent,
              )
            }
          }
          Spacer(Modifier.height(8.dp))
          Text(
            text = if (isHindi) game.titleHi else game.titleEn,
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = if (isHindi) game.ruleHi else game.ruleEn,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(10.dp))
          if (sub != null) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = "Submission Status: ${sub.status}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          } else {
            Button(
              onClick = {
                activeGame = game
                linkedTaskForGame = matchedTask
                completedProofSummary = null
                gamePassed = false
              },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
              modifier = Modifier.fillMaxWidth().testTag("play_${game.defaultTaskId}"),
            ) {
              Text(
                if (matchedTask != null) {
                  if (isHindi) "गेम खेलें और कमाएं" else "Play & Submit Proof"
                } else {
                  if (isHindi) "अभी अभ्यास खेलें" else "Play Practice Round"
                }
              )
            }
          }
        }
      }
    }

    val customGameTasks =
      gameTasks.filter { t -> EasyGameType.entries.none { it.defaultTaskId == t.id } }
    if (customGameTasks.isNotEmpty()) {
      item {
        Text(
          text = if (isHindi) "अन्य एडमिन गेम कार्य" else "More Admin Game Tasks",
          style = MaterialTheme.typography.titleMedium,
        )
      }
      items(customGameTasks, key = { it.id }) { task ->
        val sub = submissions.firstOrNull { it.taskId == task.id }
        Card(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(if (isHindi && task.titleHi.isNotBlank()) task.titleHi else task.title, style = MaterialTheme.typography.titleMedium)
            Text(task.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("Net ₹${task.workerNetInr} • Status: ${sub?.status ?: "OPEN"}", color = EmeraldAccent)
          }
        }
      }
    }
  }

  activeGame?.let { game ->
    AlertDialog(
      onDismissRequest = { activeGame = null },
      title = { Text(if (isHindi) game.titleHi else game.titleEn) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          EasyGamePlayableArea(
            gameType = game,
            isHindi = isHindi,
            onGameCompleted = { passed, summary ->
              gamePassed = passed
              completedProofSummary = summary
            },
          )
          completedProofSummary?.let { summary ->
            Text(summary, style = MaterialTheme.typography.bodyMedium)
          }
        }
      },
      confirmButton = {
        val targetTask = linkedTaskForGame
        if (completedProofSummary != null && gamePassed && targetTask != null) {
          Button(
            onClick = {
              onSubmitProof(targetTask.id, completedProofSummary!!, "")
              activeGame = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
          ) {
            Text(if (isHindi) "इनाम के लिए भेजें" else "Submit Verified Proof")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { activeGame = null }) {
          Text(if (isHindi) "बंद करें" else "Close")
        }
      },
    )
  }
}

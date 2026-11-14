package com.example.ui.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite
import kotlin.random.Random

enum class EasyGameType(
  val defaultTaskId: String,
  val titleEn: String,
  val titleHi: String,
  val ruleEn: String,
  val ruleHi: String,
) {
  QUICK_MATH(
    defaultTaskId = "game_math_01",
    titleEn = "Quick Math Sprint",
    titleHi = "आसान गणित स्प्रिंट",
    ruleEn = "Solve 5 easy sums. Get 4+ right to pass.",
    ruleHi = "5 आसान गणित सवाल हल करें। पास होने के लिए 4+ सही उत्तर दें।",
  ),
  COLOR_MATCH(
    defaultTaskId = "game_color_02",
    titleEn = "Color & Word Match",
    titleHi = "रंग और शब्द मिलान",
    ruleEn = "Check if the color box matches the word (5/6 to pass).",
    ruleHi = "जांचें कि रंग बॉक्स और लिखा शब्द मेल खाते हैं (5/6 सही आवश्यक)।",
  ),
  MEMORY_PAIRS(
    defaultTaskId = "game_memory_03",
    titleEn = "Memory Flip Pairs",
    titleHi = "मेमोरी कार्ड मिलान",
    ruleEn = "Find all 3 matching pairs in 8 moves or fewer.",
    ruleHi = "8 चालों या कम में सभी 3 जोड़ियाँ मिलाएँ।",
  ),
  NUMBER_SEQUENCE(
    defaultTaskId = "game_sequence_04",
    titleEn = "Number Sequence Puzzle",
    titleHi = "संख्या क्रम पहेली",
    ruleEn = "Complete 4 easy number patterns (3+ correct to pass).",
    ruleHi = "4 आसान नंबर पैटर्न पूरे करें (3+ सही आवश्यक)।",
  ),
  WORD_QUIZ(
    defaultTaskId = "game_word_05",
    titleEn = "Hindi-English Word Quiz",
    titleHi = "हिंदी-अंग्रेज़ी शब्द खेल",
    ruleEn = "Match 5 everyday Hindi words to English (4+ correct to pass).",
    ruleHi = "5 रोज़मर्रा के हिंदी शब्दों का अंग्रेज़ी अर्थ चुनें (4+ सही आवश्यक)।",
  ),
  TAP_TARGET(
    defaultTaskId = "game_tap_06",
    titleEn = "Tap Speed Reflex QA",
    titleHi = "तेज़ टैप रिफ्लेक्स टेस्ट",
    ruleEn = "Tap the green target 8 times cleanly to verify touch accuracy.",
    ruleHi = "टच सटीकता जांचने के लिए हरे निशान को 8 बार टैप करें।",
  ),
}

@Composable
fun EasyGamePlayableArea(
  gameType: EasyGameType,
  isHindi: Boolean,
  onGameCompleted: (passed: Boolean, summary: String) -> Unit,
) {
  when (gameType) {
    EasyGameType.QUICK_MATH -> QuickMathMiniGame(isHindi, onGameCompleted)
    EasyGameType.COLOR_MATCH -> ColorMatchMiniGame(isHindi, onGameCompleted)
    EasyGameType.MEMORY_PAIRS -> MemoryPairsMiniGame(isHindi, onGameCompleted)
    EasyGameType.NUMBER_SEQUENCE -> NumberSequenceMiniGame(isHindi, onGameCompleted)
    EasyGameType.WORD_QUIZ -> WordQuizMiniGame(isHindi, onGameCompleted)
    EasyGameType.TAP_TARGET -> TapTargetMiniGame(isHindi, onGameCompleted)
  }
}

private data class QuizQuestion(val prompt: String, val options: List<String>, val correctIndex: Int)

@Composable
private fun QuickMathMiniGame(
  isHindi: Boolean,
  onComplete: (Boolean, String) -> Unit,
) {
  val questions = remember {
    List(5) {
      val a = Random.nextInt(3, 25)
      val b = Random.nextInt(2, 18)
      val useMultiply = it == 4
      val prompt = if (useMultiply) "$a × $b = ?" else "$a + $b = ?"
      val ans = if (useMultiply) a * b else a + b
      val wrong1 = ans + Random.nextInt(1, 5)
      val wrong2 = (ans - Random.nextInt(1, 5)).coerceAtLeast(1)
      val opts = listOf(ans, wrong1, wrong2).distinct().let { list ->
        if (list.size < 3) listOf(ans, ans + 2, ans + 4) else list
      }.shuffled()
      QuizQuestion(prompt, opts.map { it.toString() }, opts.indexOf(ans))
    }
  }
  QuizRunner(
    questions = questions,
    passThreshold = 4,
    gameTitle = "Quick Math Sprint",
    isHindi = isHindi,
    onComplete = onComplete,
  )
}

@Composable
private fun NumberSequenceMiniGame(
  isHindi: Boolean,
  onComplete: (Boolean, String) -> Unit,
) {
  val questions = remember {
    listOf(
      QuizQuestion("2, 4, 6, 8, ?", listOf("9", "10", "12"), 1),
      QuizQuestion("5, 10, 15, 20, ?", listOf("25", "30", "22"), 0),
      QuizQuestion("10, 20, 30, 40, ?", listOf("45", "60", "50"), 2),
      QuizQuestion("3, 6, 9, 12, ?", listOf("15", "14", "18"), 0),
    )
  }
  QuizRunner(
    questions = questions,
    passThreshold = 3,
    gameTitle = "Number Sequence",
    isHindi = isHindi,
    onComplete = onComplete,
  )
}

@Composable
private fun WordQuizMiniGame(
  isHindi: Boolean,
  onComplete: (Boolean, String) -> Unit,
) {
  val questions = remember {
    listOf(
      QuizQuestion("पुस्तक (Pustak) = ?", listOf("Book", "Table", "Road"), 0),
      QuizQuestion("समय (Samay) = ?", listOf("Water", "Time", "Friend"), 1),
      QuizQuestion("कौशल (Kaushal) = ?", listOf("Skill", "Market", "Home"), 0),
      QuizQuestion("व्यापार (Vyapar) = ?", listOf("School", "Weather", "Business"), 2),
      QuizQuestion("सुरक्षा (Suraksha) = ?", listOf("Security", "Payment", "Speed"), 0),
    )
  }
  QuizRunner(
    questions = questions,
    passThreshold = 4,
    gameTitle = "Hindi-English Word Quiz",
    isHindi = isHindi,
    onComplete = onComplete,
  )
}

@Composable
private fun QuizRunner(
  questions: List<QuizQuestion>,
  passThreshold: Int,
  gameTitle: String,
  isHindi: Boolean,
  onComplete: (Boolean, String) -> Unit,
) {
  var index by remember { mutableIntStateOf(0) }
  var score by remember { mutableIntStateOf(0) }

  if (index >= questions.size) {
    val passed = score >= passThreshold
    Text(
      text =
        if (passed) "✅ ${if (isHindi) "पास!" else "Passed!"} Score: $score/${questions.size}"
        else "❌ Score: $score/${questions.size} (Need $passThreshold+)",
      style = MaterialTheme.typography.titleMedium,
      color = if (passed) EmeraldAccent else MaterialTheme.colorScheme.error,
    )
    return
  }

  val q = questions[index]
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = "${if (isHindi) "सवाल" else "Question"} ${index + 1}/${questions.size} • Score: $score",
      style = MaterialTheme.typography.labelMedium,
    )
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = q.prompt,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(18.dp),
      )
    }
    q.options.forEachIndexed { optIdx, label ->
      Button(
        onClick = {
          val nextScore = if (optIdx == q.correctIndex) score + 1 else score
          score = nextScore
          val nextIndex = index + 1
          index = nextIndex
          if (nextIndex >= questions.size) {
            val passed = nextScore >= passThreshold
            onComplete(passed, "$gameTitle verified score: $nextScore/${questions.size}")
          }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("game_option_$optIdx"),
      ) {
        Text(label)
      }
    }
  }
}

@Composable
private fun ColorMatchMiniGame(
  isHindi: Boolean,
  onComplete: (Boolean, String) -> Unit,
) {
  val palette = remember {
    listOf(
      Triple("GREEN", "हरा", EmeraldAccent),
      Triple("NAVY", "नीला", NavyPrimary),
      Triple("RED", "लाल", Color(0xFFD32F2F)),
      Triple("GOLD", "सुनहरा", Color(0xFFF59E0B)),
    )
  }
  val rounds = remember {
    List(6) {
      val wordIdx = Random.nextInt(palette.size)
      val shouldMatch = it % 2 == 0
      val colorIdx = if (shouldMatch) wordIdx else (wordIdx + 1) % palette.size
      Triple(palette[wordIdx], palette[colorIdx].third, shouldMatch)
    }
  }
  var index by remember { mutableIntStateOf(0) }
  var score by remember { mutableIntStateOf(0) }

  if (index >= rounds.size) {
    val passed = score >= 5
    Text(
      text = if (passed) "✅ Passed! Score: $score/6" else "❌ Score: $score/6 (Need 5+)",
      style = MaterialTheme.typography.titleMedium,
      color = if (passed) EmeraldAccent else MaterialTheme.colorScheme.error,
    )
    return
  }

  val (wordTriple, boxColor, matches) = rounds[index]
  Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Round ${index + 1}/6 • Score: $score", style = MaterialTheme.typography.labelMedium)
    Box(
      modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).background(boxColor),
    )
    Text(
      text = if (isHindi) "${wordTriple.second} (${wordTriple.first})" else wordTriple.first,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(
        onClick = {
          val nextScore = if (matches) score + 1 else score
          score = nextScore
          index += 1
          if (index >= rounds.size) onComplete(nextScore >= 5, "Color Match verified score: $nextScore/6")
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
      ) { Text(if (isHindi) "हाँ, सही है" else "Yes, Match") }
      OutlinedButton(
        onClick = {
          val nextScore = if (!matches) score + 1 else score
          score = nextScore
          index += 1
          if (index >= rounds.size) onComplete(nextScore >= 5, "Color Match verified score: $nextScore/6")
        },
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
      ) { Text(if (isHindi) "नहीं" else "No Match") }
    }
  }
}

@Composable
private fun MemoryPairsMiniGame(
  isHindi: Boolean,
  onComplete: (Boolean, String) -> Unit,
) {
  val cards = remember { listOf("₹", "₹", "★", "★", "✔", "✔").shuffled() }
  var matched by remember { mutableStateOf(setOf<Int>()) }
  var firstPick by remember { mutableStateOf<Int?>(null) }
  var secondPick by remember { mutableStateOf<Int?>(null) }
  var moves by remember { mutableIntStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = "${if (isHindi) "चालें" else "Moves"}: $moves/8 • ${if (isHindi) "जोड़ी" else "Pairs"}: ${matched.size / 2}/3",
      style = MaterialTheme.typography.labelMedium,
    )
    for (row in 0..1) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (col in 0..2) {
          val idx = row * 3 + col
          val isOpen = idx in matched || idx == firstPick || idx == secondPick
          Box(
            modifier =
              Modifier.weight(1f)
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isOpen) EmeraldAccent.copy(alpha = 0.2f) else NavyPrimary)
                .border(1.dp, NavyPrimary, RoundedCornerShape(12.dp))
                .clickable(enabled = idx !in matched && idx != firstPick && matched.size < cards.size) {
                  val f = firstPick
                  val s = secondPick
                  if (f == null || s != null) {
                    firstPick = idx
                    secondPick = null
                  } else {
                    moves += 1
                    secondPick = idx
                    if (cards[f] == cards[idx]) {
                      val nextMatched = matched + f + idx
                      matched = nextMatched
                      firstPick = null
                      secondPick = null
                      if (nextMatched.size == cards.size) {
                        onComplete(moves <= 8, "Memory Pairs solved in $moves moves (3/3 pairs)")
                      }
                    }
                  }
                },
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (isOpen) cards[idx] else "?",
              style = MaterialTheme.typography.titleLarge,
              color = if (isOpen) NavyPrimary else PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TapTargetMiniGame(
  isHindi: Boolean,
  onComplete: (Boolean, String) -> Unit,
) {
  var hits by remember { mutableIntStateOf(0) }
  var activeCell by remember { mutableIntStateOf(Random.nextInt(9)) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "${if (isHindi) "लक्ष्य टैप" else "Target Hits"}: $hits / 8",
      style = MaterialTheme.typography.labelMedium,
    )
    for (r in 0..2) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (c in 0..2) {
          val cell = r * 3 + c
          val isTarget = cell == activeCell && hits < 8
          Box(
            modifier =
              Modifier.weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isTarget) EmeraldAccent else MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = isTarget) {
                  val nextHits = hits + 1
                  hits = nextHits
                  if (nextHits >= 8) {
                    onComplete(true, "Tap Speed Reflex QA completed: 8/8 targets hit accurately")
                  } else {
                    var next = Random.nextInt(9)
                    if (next == activeCell) next = (next + 1) % 9
                    activeCell = next
                  }
                },
            contentAlignment = Alignment.Center,
          ) {
            if (isTarget) {
              Text("TAP", style = MaterialTheme.typography.labelMedium, color = PureWhite, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

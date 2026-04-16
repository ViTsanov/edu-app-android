package com.viktor.englishapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.StudentPathItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale


// ─────────────────────────────────────────────────────────────────
// Colours per module type
// ─────────────────────────────────────────────────────────────────

private object NodeColors {
    val done = Color(0xFF16A34A)
    val doneContainer = Color(0xFFDCFCE7)
    val active = Color(0xFF2563EB)
    val activeContainer = Color(0xFF2563EB)
    val retry = Color(0xFFD97706)
    val retryContainer = Color(0xFFFEF3C7)
    val locked = Color(0xFF9CA3AF)
    val lockedContainer = Color(0xFFF3F4F6)
    val track = Color(0xFFE5E7EB)
    val trackDone = Color(0xFF16A34A)
    val trackActive = Color(0xFF2563EB)
}

// ─────────────────────────────────────────────────────────────────
// Data model enriched from StudentPathItem
// ─────────────────────────────────────────────────────────────────

enum class NodeSide { LEFT, CENTER, RIGHT }

data class PathNode(
    val item: StudentPathItem,
    val index: Int,
    val side: NodeSide,
    val moduleLabel: String,
    val moduleColor: Color
)

private fun moduleFromTitle(title: String): Pair<String, Color> = when {
    title.contains("Grammar", ignoreCase = true) ||
            title.contains("Граматика", ignoreCase = true) ||
            title.contains("Present", ignoreCase = true) ||
            title.contains("Past", ignoreCase = true) ||
            title.contains("Future", ignoreCase = true) -> "Граматика" to Color(0xFF1E40AF)
    title.contains("Speak", ignoreCase = true) ||
            title.contains("Говорене", ignoreCase = true) -> "Говорене" to Color(0xFF166534)
    title.contains("Read", ignoreCase = true) ||
            title.contains("Четене", ignoreCase = true) -> "Четене" to Color(0xFF6B21A8)
    title.contains("Listen", ignoreCase = true) ||
            title.contains("Слушане", ignoreCase = true) -> "Слушане" to Color(0xFF9F1239)
    else -> "Упражнение" to Color(0xFF374151)
}

private val sidePattern = listOf(NodeSide.CENTER, NodeSide.RIGHT, NodeSide.CENTER, NodeSide.LEFT)

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class LearningPathViewModel : ViewModel() {

    var nodes by mutableStateOf<List<PathNode>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")
    var selectedNode by mutableStateOf<PathNode?>(null)
    var englishLevel by mutableStateOf("A1")
    var totalXp by mutableStateOf(0)
    var username by mutableStateOf("")

    fun load(tokenManager: TokenManager) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                // Load profile for header
                val profile = RetrofitClient.instance.getMyProfile("Bearer $token")
                englishLevel = profile.english_level ?: "A1"
                totalXp = profile.total_xp
                username = profile.username

                // Load path
                val path = RetrofitClient.instance.getMyPath("Bearer $token")
                nodes = path.mapIndexed { index, item ->
                    val (label, color) = moduleFromTitle(item.title)
                    PathNode(
                        item = item,
                        index = index,
                        side = sidePattern[index % sidePattern.size],
                        moduleLabel = label,
                        moduleColor = color
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Грешка при зареждане: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathScreen(
    onBack: () -> Unit,
    onStartExercise: (StudentPathItem) -> Unit,
    viewModel: LearningPathViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { viewModel.load(tokenManager) }

    // XP progress: each level needs 1000 XP (adjust as needed)
    val xpForLevel = 1000
    val xpProgress = (viewModel.totalXp % xpForLevel).toFloat() / xpForLevel

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Учебен път") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            viewModel.errorMessage.isNotEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {

                    // ── Header card ──────────────────────────────
                    PathHeader(
                        username = viewModel.username,
                        level = viewModel.englishLevel,
                        totalXp = viewModel.totalXp,
                        xpProgress = xpProgress,
                        xpForLevel = xpForLevel
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Section label ────────────────────────────
                    Text(
                        "Твоят учебен път — ${viewModel.englishLevel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── Path ─────────────────────────────────────
                    PathTrack(
                        nodes = viewModel.nodes,
                        selectedNodeIndex = viewModel.selectedNode?.index,
                        onNodeTap = { node ->
                            viewModel.selectedNode =
                                if (viewModel.selectedNode?.index == node.index) null
                                else node
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── Detail sheet (shows on node tap) ─────────
                    // ── Detail sheet (shows on node tap) ─────────
                    val detailAlpha by animateFloatAsState(
                        targetValue = if (viewModel.selectedNode != null) 1f else 0f,
                        animationSpec = tween(220),
                        label = "detail_alpha"
                    )
                    val detailOffset by animateDpAsState(
                        targetValue = if (viewModel.selectedNode != null) 0.dp else 24.dp,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label = "detail_offset"
                    )
                    if (viewModel.selectedNode != null || detailAlpha > 0f) {
                        viewModel.selectedNode?.let { node ->
                            NodeDetailCard(
                                node = node,
                                modifier = Modifier
                                    .alpha(detailAlpha)
                                    .offset(y = detailOffset),
                                onStart = {
                                    viewModel.selectedNode = null
                                    onStartExercise(node.item)
                                },
                                onDismiss = { viewModel.selectedNode = null }
                            )
                        }
                    }

                    // ── Level progress summary ────────────────────
                    LevelSummary(nodes = viewModel.nodes)

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Header card with XP bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PathHeader(
    username: String,
    level: String,
    totalXp: Int,
    xpProgress: Float,
    xpForLevel: Int
) {
    val animatedXp by animateFloatAsState(
        targetValue = xpProgress,
        animationSpec = tween(1000, easing = EaseOut),
        label = "xp"
    )

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Добре дошъл, $username",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                "Ниво $level",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        "$totalXp XP",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedXp },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                    color = Color(0xFFF59E0B),
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                )
                Text(
                    "до ${level.dropLast(1)}${level.last().plus(1)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Zigzag path with curved connectors drawn on Canvas
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PathTrack(
    nodes: List<PathNode>,
    selectedNodeIndex: Int?,
    onNodeTap: (PathNode) -> Unit
) {
    val nodeSize = 72.dp
    val rowHeight = 120.dp
    val screenWidth = 360.dp  // approximate phone width

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ── Canvas for curved connectors ─────────────────────────
        val totalHeight = rowHeight * nodes.size
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            val w = size.width
            val nodeHalf = nodeSize.toPx() / 2f
            val rowH = rowHeight.toPx()

            fun centerX(side: NodeSide): Float = when (side) {
                NodeSide.LEFT -> nodeHalf + 12f
                NodeSide.CENTER -> w / 2f
                NodeSide.RIGHT -> w - nodeHalf - 12f
            }

            for (i in 0 until nodes.size - 1) {
                val from = nodes[i]
                val to = nodes[i + 1]
                val y1 = i * rowH + rowH / 2f
                val y2 = (i + 1) * rowH + rowH / 2f
                val x1 = centerX(from.side)
                val x2 = centerX(to.side)

                val isDone = from.item.status == "COMPLETED"
                val isActive = to.item.status != "COMPLETED" &&
                        from.item.status == "COMPLETED"

                val color = when {
                    isDone -> NodeColors.trackDone
                    isActive -> NodeColors.trackActive
                    else -> NodeColors.track
                }

                val pathEffect = if (to.item.status == "COMPLETED" ||
                    to.item.status == "RETRY" ||
                    to.item.status == "AVAILABLE")
                    null
                else PathEffect.dashPathEffect(floatArrayOf(12f, 8f))

                drawZigzagConnector(
                    x1 = x1, y1 = y1,
                    x2 = x2, y2 = y2,
                    color = color,
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = pathEffect
                )
            }
        }

        // ── Node composables ─────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            nodes.forEachIndexed { index, node ->
                var appeared by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 60L)
                    appeared = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                    contentAlignment = when (node.side) {
                        NodeSide.LEFT -> Alignment.CenterStart
                        NodeSide.CENTER -> Alignment.Center
                        NodeSide.RIGHT -> Alignment.CenterEnd
                    }
                )
                {
                    val nodeAlpha by animateFloatAsState(
                        targetValue = if (appeared) 1f else 0f,
                        animationSpec = tween(300),
                        label = "node_alpha_$index"
                    )
                    val nodeScale by animateFloatAsState(
                        targetValue = if (appeared) 1f else 0.5f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label = "node_scale_$index"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(
                                start = if (node.side == NodeSide.LEFT) 12.dp else 0.dp,
                                end = if (node.side == NodeSide.RIGHT) 12.dp else 0.dp
                            )
                            .alpha(nodeAlpha)
                            .scale(nodeScale)
                    ) {
                        // Module chip
                        Surface(
                            color = node.moduleColor.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                node.moduleLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = node.moduleColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Node circle
                        PathNodeCircle(
                            node = node,
                            nodeSize = nodeSize,
                            isSelected = selectedNodeIndex == node.index,
                            onTap = { onNodeTap(node) }
                        )

                        // Label
                        Text(
                            node.item.title.take(18) +
                                    if (node.item.title.length > 18) "…" else "",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = when (node.item.status) {
                                "COMPLETED" -> NodeColors.done
                                "AVAILABLE" -> MaterialTheme.colorScheme.primary
                                "RETRY" -> NodeColors.retry
                                else -> MaterialTheme.colorScheme.secondary
                            },
                            modifier = Modifier
                                .widthIn(max = 80.dp)
                                .padding(top = 4.dp),
                            maxLines = 2
                        )
                    }

                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Individual node circle
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PathNodeCircle(
    node: PathNode,
    nodeSize: Dp,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse_${node.index}")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (node.item.status == "AVAILABLE") 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "pulse_scale_${node.index}"
    )

    val containerColor = when (node.item.status) {
        "COMPLETED" -> NodeColors.doneContainer
        "AVAILABLE" -> NodeColors.activeContainer
        "RETRY" -> NodeColors.retryContainer
        else -> NodeColors.lockedContainer
    }
    val borderColor = when (node.item.status) {
        "COMPLETED" -> NodeColors.done
        "AVAILABLE" -> NodeColors.active
        "RETRY" -> NodeColors.retry
        else -> NodeColors.locked
    }

    Box(contentAlignment = Alignment.Center) {
        // Selection ring
        if (isSelected) {
            Surface(
                modifier = Modifier.size(nodeSize + 12.dp),
                shape = CircleShape,
                color = borderColor.copy(alpha = 0.2f)
            ) {}
        }

        // Pulse ring for active node
        if (node.item.status == "AVAILABLE") {
            Surface(
                modifier = Modifier.size(nodeSize * pulseScale),
                shape = CircleShape,
                color = NodeColors.active.copy(alpha = 0.15f)
            ) {}
        }

        // Main circle
        Surface(
            modifier = Modifier
                .size(nodeSize)
                .clickable { onTap() },
            shape = CircleShape,
            color = containerColor,
            border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 3.dp else 2.dp,
                color = borderColor
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (node.item.status) {
                    "COMPLETED" -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = NodeColors.done,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    "AVAILABLE" -> {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    "RETRY" -> {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = NodeColors.retry,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = NodeColors.locked,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Score badge on completed nodes
        if (node.item.status == "COMPLETED" && node.item.best_score != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = NodeColors.done
            ) {
                Text(
                    "${node.item.best_score}/100",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
        }

        // Retry badge
        if (node.item.status == "RETRY") {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                shape = CircleShape,
                color = NodeColors.retry
            ) {
                Text(
                    "!",
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Canvas helper — curved zigzag connector
// ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawZigzagConnector(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    color: Color,
    strokeWidth: Float,
    pathEffect: PathEffect?
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(x1, y1)
        val cp1x = x1
        val cp1y = (y1 + y2) / 2f
        val cp2x = x2
        val cp2y = (y1 + y2) / 2f
        cubicTo(cp1x, cp1y, cp2x, cp2y, x2, y2)
    }
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = pathEffect
        )
    )
}

// ─────────────────────────────────────────────────────────────────
// Node detail card (slides in on tap)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun NodeDetailCard(
    node: PathNode,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    val item = node.item

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (item.status) {
                            "COMPLETED" -> "Завършен с ${item.best_score}/100"
                            "RETRY" -> "Последен резултат: ${item.best_score}/100 — нужни 70+"
                            "AVAILABLE" -> "Готов за решаване"
                            else -> "Завърши предишните уроци, за да отключиш"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Затвори", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            when (item.status) {
                "AVAILABLE" -> {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Започни упражнението", fontWeight = FontWeight.Bold)
                    }
                }
                "RETRY" -> {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NodeColors.retry
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Опитай пак", fontWeight = FontWeight.Bold)
                    }
                }
                "COMPLETED" -> {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Виж отговорите", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = false
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Заключено")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Level progress summary at the bottom
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LevelSummary(nodes: List<PathNode>) {
    val completed = nodes.count { it.item.status == "COMPLETED" }
    val total = nodes.size
    val avgScore = if (completed > 0)
        nodes.filter { it.item.status == "COMPLETED" }
            .mapNotNull { it.item.best_score }
            .average().toInt()
    else 0

    val progress = if (total > 0) completed.toFloat() / total else 0f
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOut),
        label = "level_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Напредък на ниво",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetricItem("$completed/$total", "Завършени")
                SummaryMetricItem(if (completed > 0) "$avgScore/100" else "—", "Среден резултат")
                SummaryMetricItem("${(progress * 100).toInt()}%", "Ниво")
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun SummaryMetricItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
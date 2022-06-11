package com.example.classattendanceapp.presenter.screens.subjectsscreen

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classattendanceapp.data.models.Subject
import com.example.classattendanceapp.domain.models.ModifiedSubjects
import com.example.classattendanceapp.presenter.viewmodel.ClassAttendanceViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun SubjectsScreen(
    classAttendanceViewModel: ClassAttendanceViewModel
){

    val subjectsList = remember{
        mutableStateListOf<ModifiedSubjects>()
    }

    val coroutineScope = rememberCoroutineScope()

    val showAddSubjectDialog = classAttendanceViewModel.floatingButtonClicked.collectAsState()

    var subjectNameTextField by remember{
        mutableStateOf("")
    }

    LaunchedEffect(Unit){
        classAttendanceViewModel.getSubjects().collectLatest{
            subjectsList.clear()
            subjectsList.addAll(it)
        }
    }


    // Alert Dialog -> To add new subject
    if(showAddSubjectDialog.value){
        AlertDialog(
            onDismissRequest = {
                classAttendanceViewModel.changeFloatingButtonClickedState(state = false)
                subjectNameTextField = ""
            },
            text = {
                Column(){
                    Text(
                        text = "Add new Subject",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        modifier = Modifier.padding(10.dp),
                        value = subjectNameTextField,
                        onValueChange = { subjectNameTextField = it },
                        placeholder = {
                            Text("Subject Name")
                        },
                        maxLines = 1
                    )
                }
            },
            buttons = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    contentAlignment = Alignment.CenterEnd
                ){
                    Row{
                        TextButton(
                            onClick = {
                                coroutineScope.launch{
                                    classAttendanceViewModel.insertSubject(Subject(0, subjectNameTextField))
                                    subjectNameTextField = ""
                                }
                                classAttendanceViewModel.changeFloatingButtonClickedState(state = false)
                            }
                        ) {
                            Text("Add")
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        TextButton(
                            onClick = {
                                classAttendanceViewModel.changeFloatingButtonClickedState(state = false)
                                subjectNameTextField = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        )
    }

    // Original Ui
    Box(
        modifier = Modifier.fillMaxSize()
    ){
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            items(subjectsList){
                val dismissState = rememberDismissState()

                SwipeToDismiss(
                    state = dismissState,
                    background = {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(10.dp)
                                .background(Color.Red)
                        ){
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart
                            ){
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(10.dp)
                            .animateItemPlacement()
                    ) {
                        var showOverFlowMenu by remember{ mutableStateOf(false) }
                        DropdownMenu(
                            expanded = showOverFlowMenu,
                            onDismissRequest = {
                                showOverFlowMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    coroutineScope.launch{
                                        classAttendanceViewModel.deleteSubject(it._id)
                                        classAttendanceViewModel.deleteLogsWithSubjectId(it._id)
                                    }
                                    showOverFlowMenu = false
                                }
                            ) {
                                Text("Delete")
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .combinedClickable(
                                    onClick = {
                                        Log.d("debugging", "clicked once")
                                    },
                                    onLongClick = {
                                        Log.d("debugging", "Clicked for long")
                                        showOverFlowMenu = true
                                    }
                                ),
                            contentAlignment = Alignment.CenterStart
                        ){
                            Text(it.subjectName)
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ){

                                Box(
                                    modifier = Modifier.size(60.dp),
                                    contentAlignment = Alignment.Center
                                ){
                                    var startAnimation by remember{mutableStateOf(false)}
                                    val target = animateFloatAsState(
                                        targetValue = if(startAnimation) it.attendancePercentage.toFloat() else 0f,
                                        animationSpec = tween(
                                            durationMillis = 1000,
                                            delayMillis = 50
                                        )
                                    )

                                    val arcColor = animateColorAsState(
                                        targetValue = if(target.value < 75f) Color.Red else Color.Green
                                    )
                                    Canvas(modifier = Modifier.size(60.dp)){
                                        drawArc(
                                            color = arcColor.value,
                                            startAngle = 270f,
                                            sweepAngle = 360 * target.value/100,
                                            useCenter = false,
                                            size = this.size,
                                            style = Stroke(15f, cap = StrokeCap.Round)
                                        )
                                    }
                                    Text("${String.format("%.2f", target.value)}%")

                                    LaunchedEffect(key1 = Unit){
                                        startAnimation = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


package xyz.scootaloo.vertlin.boot.util

import xyz.scootaloo.vertlin.boot.core.currentTimeMillis

/**
 * @author flutterdash@qq.com
 * @since 2022/7/25 下午9:47
 */
class StopWatch {

    private var taskMapper = HashMap<String, TaskInfo>()

    fun start(name: String = "") {
        val task = taskMapper[name]
        if (task != null) {
            throw IllegalStateException("当前已存在计时任务, 无法重复创建")
        }

        val newTask = TaskInfo()
        taskMapper[name] = newTask
    }

    fun stop(name: String): Double {
        val task = taskMapper[name] ?: throw IllegalStateException("当前容器内没有名为'$name'的计时任务")
        if (task.finished) {
            throw IllegalStateException("该计时任务已经完成, 不能重复标记")
        }
        task.timeMills = currentTimeMillis() - task.startTime
        task.finished = true
        return prettyTotalTimeSeconds(task.timeMills)
    }

    fun clear() {
        taskMapper.clear()
    }

    private fun prettyTotalTimeSeconds(time: Long): Double {
        return time / 1000.0
    }

    private class TaskInfo(
        var finished: Boolean = false,
        val startTime: Long = currentTimeMillis(),
        var timeMills: Long = startTime
    )

}

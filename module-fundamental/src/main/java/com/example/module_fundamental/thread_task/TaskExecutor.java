package com.example.module_fundamental.thread_task;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;


import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskExecutor {

    private static final String TAG = TaskExecutor.class.getSimpleName();

    public static void executeUserTask(Object tag, Task task) {
        executeTask(ThreadUtils.TYPE_RIGHT_NOW, tag, task);
    }

    public static void executeInnerTask(Object tag, Task task) {
        executeTask(ThreadUtils.TYPE_INNER, tag, task);
    }

    public static void executeLocalTask(Object tag, Task task) {
        executeTask(ThreadUtils.TYPE_RIGHT_NOW_LOCAL, tag, task);
    }

    public static void executeMessageTask(Object tag, Task task) {
        executeTask(ThreadUtils.TYPE_STATISTICS, tag, task);
    }

    private static void executeTask(int type, Object tag, Task task) {
        if (tag == null) {
            throw new IllegalArgumentException("tag is null");
        }

        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }

        task.onPreTask();
        task.tag = tag;
        ThreadUtils.execute(type, task);

        runningTasks.compute(tag, (k, existingTasks) -> {
           List<Task> taskList = existingTasks != null ? existingTasks : new CopyOnWriteArrayList<>();
           taskList.add(task);
           return taskList;
        });
    }

    public static void cancelAllTasksByTag(Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag is null");
        }

        runningTasks.computeIfPresent(tag, (k, tasks) -> {
            tasks.forEach(task -> task.cancel(true));
            return null;
        });
     }

     public static void cancelAllTasks() {
         Set<Object> keys = runningTasks.keySet();
         for (Object tag: keys) {
             cancelAllTasksByTag(tag);
         }
     }


    public static void cancelSpecificTask(Object tag, Task task) {
        if (tag == null) {
            throw new IllegalArgumentException("tag is null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        task.cancel(true);
        List<Task> tasks = runningTasks.get(tag);
        if (tasks != null) {
            try {
                tasks.remove(task);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, e.toString());
            }
            if (tasks.isEmpty()) {
                runningTasks.remove(tag);
            }
        }
    }


    private static final Map<Object, List<Task>> runningTasks = new ConcurrentHashMap<>();


    public static abstract class Task<Params, Progress, Result> implements Runnable, IInterruptable {
        private static TaskHandler handler;
        private volatile boolean isCancelled;
        private volatile boolean isInterrupted;
        private Params[] mParams;
        private Object tag;
        private volatile long threadId;

        protected abstract Result executeTask(Params... paramsArr) throws Exception;

        public Task() {
            this.isCancelled = false;
            this.isInterrupted = false;
        }

        public Task(Params... params) {
            this();
            this.mParams = params;
        }

        private static class AsyncResult<Params, Progress> {
            Throwable exception;
            Progress[] progress;
            Params result;
            Task task;

            private AsyncResult() {

            }
        }

        public void cancel(boolean interrupt) {
            if (this.isCancelled) {
                return;
            }
            this.isCancelled = true;
            if (interrupt && !this.isInterrupted) {
                interrupt();
            }
        }

        public boolean isCancelled() {
            return this.isCancelled;
        }

        protected static Handler getHandler() {
            if (handler == null) {
                synchronized (TaskExecutor.class) {
                    if (handler == null) {
                        handler = new TaskHandler();
                    }
                }
            }
            return handler;
        }

        public void finish() {
            if (this.tag == null) {
                return;
            }

            if (isCancelled()) {
                AsyncResult<Result, Progress> result = new AsyncResult<>();
                result.task = this;
                Message message = Message.obtain();
                message.what = TaskHandler.MSG_TYPE_CANCLE;
                message.obj = result;
                getHandler().sendMessage(message);
            }
            List<Task> tasks = TaskExecutor.runningTasks.get(this.tag);
            if (tasks != null) {
                try {
                    tasks.remove(this);
                } catch (UnsupportedOperationException e) {
                    Log.e("TAG", Objects.requireNonNull(e.getMessage()));
                }
                if (tasks.isEmpty()) {
                    TaskExecutor.runningTasks.remove(this.tag);
                }
            }
        }

        @Override
        public void interrupt() {
            this.isInterrupted = true;
        }

        @Override
        public void run() {
            if (this.isInterrupted) {
                finish();
                return;
            }

            long startTime = System.currentTimeMillis();
            AsyncResult<Result, Progress> result = doInBackground(this.mParams);
            Log.i(TAG, "task[" + getClass().getName() + "] / thread[" + Thread.currentThread().getName() + "] : doInBackground costs " + (System.currentTimeMillis() - startTime));

            if (this.isInterrupted) {
                finish();
                return;
            }
            Message message = Message.obtain();
            message.what = TaskHandler.MSG_TYPE_POST_EXECUTE;
            message.obj = result;
            getHandler().sendMessage(message);
        }

        private AsyncResult<Result, Progress> doInBackground(Params... paramsArr) {
            AsyncResult<Result, Progress> asyncResult = new AsyncResult<>();
            try {
                if (!isCancelled()) {
                    this.threadId = Thread.currentThread().getId();
                    asyncResult.result = executeTask(paramsArr);
                } else {
                    asyncResult.exception = new Exception("task already canceled");
                }
            } catch (Throwable throwable) {
                asyncResult.exception = throwable;
            }
            asyncResult.task = this;
            return asyncResult;
        }

        private static class TaskHandler extends Handler {
            public static final int MSG_TYPE_POST_EXECUTE = 1;
            public static final int MSG_TYPE_PROGRESS_UPDATE = 2;
            public static final int MSG_TYPE_CANCLE = 3;

            public TaskHandler() {
                super(Looper.getMainLooper());
            }

            @Override
            public void handleMessage(@NonNull Message msg) {
                AsyncResult<?, ?> result = (AsyncResult<?, ?>) msg.obj;
                if (result == null || result.task == null) {
                    Log.i(TAG, "task[null] / thread[" + Thread.currentThread().getName() + "] : handleMessage return");
                    return;
                }
                Task task = result.task;
                switch (msg.what) {
                    case MSG_TYPE_POST_EXECUTE:
                        if (result.task.isInterrupted) {
                            Log.i(TAG, "task[" + result.task.getClass().getName() + "] / thread[" + Thread.currentThread().getName() + "] : handleMessage isInterrupted, finish");
                            result.task.finish();
                            return;
                        }
                        Log.i(TAG, "task[" + result.task.getClass().getName() + "] / thread[" + Thread.currentThread().getName() + "] : handleMessage onPostExecute");
                        task.onPostExecute(result);
                        break;
                    case MSG_TYPE_PROGRESS_UPDATE:
                        if (!result.task.isInterrupted) {
                            task.onProgressUpdate(result.progress);
                        }
                        break;
                    case MSG_TYPE_CANCLE:
                        task.onCancelled();
                        break;
                }
            }
        }

        protected final void publishProgress(Progress... progress) {
            if (!isCancelled()) {
                AsyncResult<Result, Progress> result = new AsyncResult<>();
                result.progress = progress;
                result.task = this;
                Message message = Message.obtain();
                message.what = 2;
                message.obj = result;
                getHandler().sendMessage(message);
            }
        }


        public void onPostExecute(AsyncResult<Result, Progress> result) {
            finish();
            onTaskFinish();
            if (result.exception == null) {
                onTaskSuccess(result.result);
            } else if (result.exception instanceof Exception) {
                onTaskError((Exception) result.exception);
            } else {
                onTaskError(new Exception(result.exception));
            }
        }

        protected void onCancelled() {

        }

        protected void onPreTask() {

        }


        protected void onProgressUpdate(Progress... values) {

        }

        protected void onTaskFinish() {
        }

        protected void onTaskSuccess(Result result) {
        }

        protected void onTaskError(Exception e) {

        }
    }
}


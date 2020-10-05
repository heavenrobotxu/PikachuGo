# Pikachu  
![](https://s1.ax1x.com/2020/10/05/0YE5i4.jpg)
## An Android Download Library

### 介绍
<table>
	<tr>
		<td>
    	<center>
      	<img src="https://s1.ax1x.com/2020/10/05/0YAGuV.jpg" width = "270" height = “480” /></center>
    </td>
		<td>
    	<center>
      	<img src="https://s1.ax1x.com/2020/10/05/0YAJBT.jpg" width = "270" height = “480”/>
      </center>
  	</td>
		<td>
    	<center>
      	<img src="https://s1.ax1x.com/2020/10/05/0YA3j0.jpg" width = "270" height = “480”/>
    	</center>
    </td>
	</tr>
</table>



一个简单易用的Android下载库(kotlin)，支持HTTP/HTTPS/磁力链接/BT下载，支持断点续传、支持对下载任务的多种操作（持续完善、优化功能中~）

### 接入

请在对应模块的build.gradle文件中引入依赖

```groovy
implementation 'damiao.hr:pikachu:0.0.1'
```
### 使用

#### 请先在应用的Application中初始化Pikachu

```kotlin
class App : Application() {

    override fun onCreate() {
        super.onCreate()
      
        Pikachu.init(this)
    }
}
```

#### 基础使用

```kotlin
Pikachu.with(this)
	.url("https://xxxxx/ssss.mp4")
	//.targetPath("sdcard/download/pp")
	.taskProcessListener(object : PKTaskProcessListener {
		override fun onComplete(downloadTask: PKDownloadTask) {
                    Log.d("PIKACHU", "任务下载完成，本地文件路径 ${downloadTask.downloadResultFile?.path}")
                }
       })
       .download()
```
如果不关心下载过程，仅需要使用pikachu下载某个目标文件，并在下载完成后拿到下载到本地的文件路径，那么上面这个简单的示例就可以完全满足要求，目标文件会默认下载到应用在存储卡私有目录的download目录中（Environment.DIRECTORY_DOWNLOADS）,该目录不需要显式的声明任何权限，当然您也可以选择自定义任务的下载目录（上面例子中注释掉的指定下载文件夹代码），但要注意，如果是需要显式赋予读写权限的目录，pikachu不会自动帮您申请权限，仅仅会校验是否获取了权限，应用程序需要在指定之前先请求SD卡的读写权限。

#### 操作下载任务

如果需要对下载过程有更精细的操作，例如暂停、恢复、取消下载任务，以及实时展示下载任务的下载进度等需求，那么可以像下面这样使用

```kotlin
val downloadTask = Pikachu.with(this)
    .url("magnet:?xt=urn:btih:902029de02ac1804f073dff472913008f35cfe7f")
    .taskProcessListener(object : PKTaskProcessListener {
        //任务进度回调，单位是字节
        override fun onProcess(
            process: Long,
            length: Long,
            downloadTask: PKDownloadTask
        ) {
            Log.d("PIKACHU", "任务正在下载，已下载 $process 总大小 $length")
        }
        override fun onComplete(downloadTask: PKDownloadTask) {
            Log.d("PIKACHU", "任务下载完成，本地文件路径 ${downloadTask.downloadResultFile?.path}")
        }
        override fun onCancel(downloadTask: PKDownloadTask) {
            Log.d("PIKACHU", "下载任务已经被取消")
        }
    })
    .download()

btn_pause.setOnClickListener {
  //暂停当前下载任务的下载
  downloadTask?.pause()
}

btn_resume.setOnClickListener {
  //恢复当前下载任务的下载
  downloadTask?.resume()
}

btn_cancel.setOnClickListener {
  //取消当前下载任务的下载
  downloadTask?.cancel()
}
```

上面展示的都是对单任务的监听，如果需要监听使用pikachu下载的所有任务的实时进度以及其他回调（例如展示一个下载任务列表，并实时更新下载任务进度），可以使用全局监听:

```kotlin
//全局任务listener，任何任务的回调触发时，都会触发该回调
val taskProcessListener = object : PKTaskProcessListener {
  
    override fun onReady(downloadTask: PKDownloadTask) {
        Log.d("PIKACHU", "任务${downloadTask.pkRequest.url}已经提交到准备列表")
    }

    override fun onStart(downloadTask: PKDownloadTask) {
        Log.d("PIKACHU", "任务${downloadTask.pkRequest.url}已经开始下载")
    }

    override fun onProcess(process: Long, length: Long, downloadTask: PKDownloadTask) {
         Log.d("PIKACHU", "任务${downloadTask.downloadFileName}正在下载")
    }

    override fun onComplete(downloadTask: PKDownloadTask) {
        Log.d("PIKACHU", "任务${downloadTask.downloadFileName}已经下载完成")
    }

    override fun onFail(
        reason: String,
        exception: Exception?,
        downloadTask: PKDownloadTask
    ) {
        Log.d("PIKACHU", "任务${downloadTask.pkRequest.url}下载失败，失败原因 $reason")
    }

    override fun onCancel(downloadTask: PKDownloadTask) {
        Log.d("PIKACHU", "任务${downloadTask.pkRequest.url}被取消")
    }
}
//添加任务进度全局监听
Pikachu.addGlobalTaskProcessListener(taskProcessListener, lifecycle = this)
```

#### 断点续传

在下载过程中，若应用被杀死，那么在当应用重启后可以使用以下函数获取到之前还未下载完成的PKDownloadTask并恢复下载

```kotlin
//获取未完成的任务列表
val unCompleteTaskList = Pikachu.pkTaskGetter.getUnCompleteTaskList()
for (task in unCompleteTaskList) {
    //调用resume即可恢复下载
    task.resume()
}
//如果需要展示更多任务，例如已失败、已取消或是全部下载任务，可以选择PKTaskGetter接口中的其他功能函数
val allTask = Pikachu.pkTaskGetter.getAllTaskList()
val failedTask = Pikachu.pkTaskGetter.getFailedTask()
...
```

#### 更多

更多的其他用法，包括PKDownloadTask的具体状态值等请参考demo app : PikachuGo中的代码，感谢支持~

### 感谢

https://github.com/square/okhttp

https://github.com/aldenml/libtorrent4j

License
--------

    Copyright 2020 Xudamiao
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

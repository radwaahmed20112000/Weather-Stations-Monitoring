import os
import time
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from index_parquet import index_parquet
class NewFileHandler(FileSystemEventHandler):
    def on_created(self, event):
        if not event.is_directory:
            print("New file created:", event.src_path)
            index_parquet(event.src_path)

def watch_folder(path):
    event_handler = NewFileHandler()
    observer = Observer()
    observer.schedule(event_handler, path, recursive=True)
    observer.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()

    observer.join()

if __name__ == '__main__':
    watch_folder('/home/sarah/Music/haha/baba')
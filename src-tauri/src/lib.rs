use std::process::Command;

#[tauri::command]
fn launch_core_engine(project_path: String) {
    // Spawns './gradlew run --args="..."' from the project root directory
    let _child = Command::new("./gradlew")
        .arg("run")
        .arg(format!("--args={}", project_path))
        .current_dir("../")
        .spawn()
        .expect("Failed to initialize Java Engine Core subprocess");
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }
            Ok(())
        })
        // Register the invoke handler command to expose it to the JavaScript frontend
        .invoke_handler(tauri::generate_handler![launch_core_engine])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

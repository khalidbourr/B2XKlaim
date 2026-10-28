// Processes of the open project, kept in sync by App.vue.
//
// The properties panel is built once and cannot read Vue state, so a call
// activity's "Called element" list is taken from here. Each entry carries the
// key the backend uses for that process (App.vue sends the tab key, or the
// sanitized name for callActivity_* sub-processes): that key is what has to be
// written into calledElement for the translation to find the process.

let state = { processes: [], activeKey: null };

export function setProjectProcesses(processes, activeKey) {
  state = { processes: processes || [], activeKey: activeKey || null };
}

export function getProjectProcesses() {
  return state.processes.slice();
}

export function getActiveProcessKey() {
  return state.activeKey;
}

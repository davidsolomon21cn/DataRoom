export interface VisualScreenKeyboardShortcut {
  feature: string
  windows: string
  macos: string
}

export const visualScreenKeyboardShortcuts: VisualScreenKeyboardShortcut[] = [
  {
    feature: '撤销',
    windows: 'Ctrl + Z',
    macos: 'Cmd + Z',
  },
  {
    feature: '恢复',
    windows: 'Ctrl + Y',
    macos: 'Cmd + Y',
  },
  {
    feature: '临时拖动画布',
    windows: '按住 Space 后拖动',
    macos: '按住 Space 后拖动',
  },
  {
    feature: '缩放画布',
    windows: 'Ctrl + 鼠标滚轮',
    macos: 'Cmd + 鼠标滚轮',
  },
  {
    feature: '多选或追加选择组件',
    windows: 'Ctrl + 点击组件',
    macos: 'Cmd + 点击组件',
  },
]

// 根据组件的type来返回组件的默认的完整的交互信息
function getInteractionInstall (type) {
  switch (type) {
    case 'BaseBar':
      return import(/* webpackChunkName: "BaseBarConfig" */ '@gcpaas/data-room-ui/packages/components/G2Plots/BaseBar/interaction/index')
    case 'GroupBar':
      return import(/* webpackChunkName: "BaseBarConfig" */ '@gcpaas/data-room-ui/packages/components/G2Plots/GroupBar/interaction/index')
    case 'texts':
      return import(/* webpackChunkName: "BaseBarConfig" */ '@gcpaas/data-room-ui/packages/components/texts/interaction/index')
  }
}
export function getInteraction (type) {
  return new Promise((resolve, reject) => {
    getInteractionInstall(type)
      .then((configModule) => {
        resolve(configModule.default)
      })
      .catch((error) => {
        reject(error)
      })
  })
}
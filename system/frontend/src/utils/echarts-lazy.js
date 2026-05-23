/**
 * ECharts 按需加载工具（替代全量 import('echarts')）
 *
 * 职责：
 *   - 动态导入 echarts/core 并注册所需组件
 *   - 减小 vendor-echarts chunk 体积（~0.7MB → ~0.3MB）
 *   - 单例 Promise 缓存，避免重复加载
 *   - 导入失败后自动重置缓存，下次调用可重试（修复：已修复失败后永久缓存旧 Promise 的 bug）
 *
 * 使用方式（在 DashboardPage.vue / AnalyticsPage.vue 中）：
 *   import { loadEcharts } from '../utils/echarts-lazy'
 *   const echarts = await loadEcharts()
 *   const chart = echarts.init(dom)
 *
 * 包含组件（按需加载，减少未使用图表类型的死代码）：
 *   图表类型: PieChart（饼图）, BarChart（柱状图）, LineChart（折线图）
 *   坐标系:   GridComponent（直角坐标系）
 *   交互组件: TooltipComponent（提示框）, LegendComponent（图例）, TitleComponent（标题）
 *   渲染器:   CanvasRenderer（Canvas 渲染方式）
 */
let echartsPromise = null                                  // 单例Promise缓存：首次加载后复用，避免重复import

/**
 * 加载并初始化 ECharts（按需版本，替代全量导入）
 *
 * 首次调用时执行动态 import + 组件注册，后续调用直接返回缓存的 Promise。
 * 加载顺序：echarts/core → 并行加载 charts/components/renderers → use() 注册 → 返回核心引用
 * 导入失败时自动清除缓存，下次调用可重新尝试（修复：网络错误后图表不永久损坏）
 *
 * @returns {Promise<Object>} echarts 核心模块引用（含 init/dispose/use 等方法）
 */
export function loadEcharts() {                            // 加载 ECharts 核心 + 按需组件
  if (!echartsPromise) {                                   // 首次调用或上一次失败已清除缓存时才执行加载
    echartsPromise = import('echarts/core').then(async (echarts) => { // 动态导入 echarts 核心模块（echarts/core）
      // 并行加载三个子模块（charts + components + renderers），Promise.all 等待全部完成
      const [
        { PieChart, BarChart, LineChart },                 // 图表类型组件（echarts/charts 模块）
        { GridComponent, TooltipComponent, LegendComponent, TitleComponent }, // 交互/坐标系/标题/图例/提示框组件（echarts/components 模块）
        { CanvasRenderer }                                 // Canvas 2D 渲染器（echarts/renderers 模块，不依赖 SVG DOM）
      ] = await Promise.all([                              // 并行异步加载，减少总等待时间
        import('echarts/charts'),                          // 图表模块: PieChart/BarChart/LineChart
        import('echarts/components'),                      // 组件模块: Grid/Tooltip/Legend/Title
        import('echarts/renderers')                        // 渲染器模块: CanvasRenderer
      ])
      echarts.use([                                        // 注册所有组件到 echarts 核心（必须在使用前注册）
        PieChart, BarChart, LineChart,                     // 三种图表类型
        GridComponent,                                     // 直角坐标系（柱状图/折线图必需）
        TooltipComponent, LegendComponent, TitleComponent,  // 交互提示/图例/标题
        CanvasRenderer                                    // Canvas 渲染器
      ])
      return echarts                                       // 返回初始化完毕的 echarts 核心引用
    }).catch(err => {                                      // 修复：导入失败时清除缓存，下次调用可重试
      echartsPromise = null                                // 清除失败缓存（原 bug：永久缓存 rejected Promise 导致图表永远无法渲染）
      throw err                                            // 继续向上传播错误，供调用方处理
    })
  }
  return echartsPromise                                    // 返回缓存的 Promise（非首次调用时已预加载完毕）
}

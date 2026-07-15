const fs = require('node:fs/promises')
const path = require('node:path')
const { chromium } = require('playwright')

const baseUrl = process.env.FINANCE_BASE_URL || 'http://localhost:5173'
const password = process.env.FINANCE_TEST_PASSWORD
const outputDir = path.resolve(__dirname, 'system_screenshots')

if (!password) {
  throw new Error('FINANCE_TEST_PASSWORD is required')
}

const pages = [
  ['dashboard', '/', 'main h2'],
  ['account', '/account', 'main h2'],
  ['category', '/category', 'main h2'],
  ['transaction', '/transaction', 'main h2'],
  ['budget', '/budget', 'main h2'],
  ['recurring-bill', '/recurring-bill', 'main h2'],
  ['transfer', '/transfer', 'main h2'],
  ['analytics', '/analytics', 'main h2'],
  ['import', '/import', 'main h2'],
  ['settings', '/settings', 'main h2'],
]

async function waitForPage(page, selector) {
  await page.waitForLoadState('domcontentloaded')
  await page.locator(selector).waitFor({ state: 'visible', timeout: 15000 })
  await page.waitForTimeout(1200)
}

async function login(page, username) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' })
  const loginPanel = page.getByRole('tabpanel', { name: '登录' })
  await loginPanel.getByPlaceholder('请输入用户名').fill(username)
  await loginPanel.getByPlaceholder('请输入密码').fill(password)
  await Promise.all([
    page.waitForURL(`${baseUrl}/`, { timeout: 15000 }),
    loginPanel.getByRole('button', { name: '登录', exact: true }).click(),
  ])
  await waitForPage(page, 'main h2')
}

async function saveScreenshot(page, name) {
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.screenshot({
    path: path.join(outputDir, `${name}.png`),
    fullPage: false,
    animations: 'disabled',
  })
}

async function main() {
  await fs.mkdir(outputDir, { recursive: true })
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1.5,
    locale: 'zh-CN',
  })
  const page = await context.newPage()

  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' })
  await page.getByRole('heading', { name: '个人财务记账与分析系统' }).waitFor()
  await page.waitForTimeout(500)
  await saveScreenshot(page, 'login')

  await login(page, 'zhangsan')
  for (const [name, route, selector] of pages) {
    await page.goto(`${baseUrl}${route}`, { waitUntil: 'domcontentloaded' })
    await waitForPage(page, selector)
    await saveScreenshot(page, name)
  }

  await context.clearCookies()
  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => localStorage.clear())
  await login(page, 'admin')
  await page.goto(`${baseUrl}/admin`, { waitUntil: 'domcontentloaded' })
  await waitForPage(page, 'main h2')
  await saveScreenshot(page, 'admin')

  await browser.close()
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})

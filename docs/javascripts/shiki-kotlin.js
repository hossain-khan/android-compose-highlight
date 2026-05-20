import { codeToHtml } from 'https://esm.sh/shiki@4.1.0'

async function highlightKotlinBlocks() {
  const wrappers = document.querySelectorAll(
    'div.language-kotlin.highlight, div.language-kt.highlight'
  )

  if (wrappers.length === 0) return

  for (const wrapper of wrappers) {
    if (wrapper.hasAttribute('data-shiki-done')) continue

    const pre = wrapper.querySelector('pre')
    if (!pre) continue

    const block = pre.querySelector('code')
    if (!block) continue

    const code = block.textContent

    try {
      const html = await codeToHtml(code, {
        lang: 'kotlin',
        // Themes: change these to switch Shiki color schemes.
        // Browse all options at https://shiki.style/themes
        // Other themes tried:
        //   light: 'github-light', 'horizon-bright'
        //   dark:  'github-dark',  'horizon'
        themes: {
          light: 'one-light',
          dark: 'one-dark-pro',
        },
        defaultColor: false,
      })

      const tempDiv = document.createElement('div')
      tempDiv.innerHTML = html

      const shikiPre = tempDiv.querySelector('pre')
      if (!shikiPre) continue

      const shikiCode = shikiPre.querySelector('code')
      if (!shikiCode) continue

      // Swap just the code innerHTML and apply Shiki's CSS variables to existing elements.
      // This preserves the DOM structure that Zensical's bundle uses for copy buttons.
      block.innerHTML = shikiCode.innerHTML

      // Apply Shiki's CSS custom properties to the pre without removing existing styles
      const shikiStyle = shikiPre.getAttribute('style') || ''
      const cssVars = shikiStyle.match(/--shiki[^;]+;?/g) || []
      pre.style.cssText += cssVars.join('')
      pre.setAttribute('data-shiki', 'true')

      wrapper.setAttribute('data-shiki-done', 'true')
    } catch (e) {
      console.warn('[shiki-kotlin] Failed to highlight block:', e)
    }
  }
}

// Run on initial page load
highlightKotlinBlocks()

// Re-run on SPA navigation (Zensical/Material instant navigation)
if (typeof document$ !== 'undefined') {
  document$.subscribe(() => highlightKotlinBlocks())
} else {
  const waitForDocStream = setInterval(() => {
    if (typeof document$ !== 'undefined') {
      document$.subscribe(() => highlightKotlinBlocks())
      clearInterval(waitForDocStream)
    }
  }, 200)
  setTimeout(() => clearInterval(waitForDocStream), 10000)
}

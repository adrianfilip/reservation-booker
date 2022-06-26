import {resolve} from 'path'
import {minifyHtml, injectHtml} from 'vite-plugin-html'

const scalaVersion = '3.1.1'

export default ({mode}) => {
	const mainJS = `./target/scala-${scalaVersion}/booker-ui-${mode === 'production' ? 'opt' : 'fastopt'}/main.js`
	const script = `<script type="module" src="${mainJS}"></script>`

	return {
		publicDir: './src/main/static/public',
		plugins: [
			...(process.env.NODE_ENV === 'production' ? [minifyHtml(),] : []),
			injectHtml({
				injectData: {
					script
				}
			})
		],
		resolve: {
			alias: {
				'stylesheets': resolve(__dirname, './src/main/static/stylesheets'),
			}
		}
	}
}
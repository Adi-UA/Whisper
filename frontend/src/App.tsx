import { ChakraProvider, extendTheme } from '@chakra-ui/react'
import { Dashboard } from './components/Dashboard'

const theme = extendTheme({
  config: { initialColorMode: 'dark', useSystemColorMode: false },
})

export default function App() {
  return (
    <ChakraProvider theme={theme}>
      <Dashboard />
    </ChakraProvider>
  )
}

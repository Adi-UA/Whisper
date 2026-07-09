import { ChakraProvider, extendTheme } from '@chakra-ui/react'
import { Dashboard } from './components/Dashboard'
import { SignedOut } from './components/SignedOut'

const theme = extendTheme({
  config: { initialColorMode: 'dark', useSystemColorMode: false },
})

export default function App() {
  const path = window.location.pathname

  return (
    <ChakraProvider theme={theme}>
      {path === '/signed-out' ? <SignedOut /> : <Dashboard />}
    </ChakraProvider>
  )
}

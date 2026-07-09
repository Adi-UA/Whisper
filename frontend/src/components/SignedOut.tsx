import { Box, Button, Heading, Text, VStack } from '@chakra-ui/react'

export function SignedOut() {
  return (
    <Box minH="100vh" bg="gray.900" color="white" display="flex" alignItems="center" justifyContent="center">
      <VStack spacing={4}>
        <Heading size="xl">🤫</Heading>
        <Heading size="md">Signed out</Heading>
        <Text color="gray.400">Your session has been ended.</Text>
        <Button colorScheme="blue" onClick={() => { window.location.href = '/oauth2/authorization/google' }}>
          Sign in again
        </Button>
      </VStack>
    </Box>
  )
}

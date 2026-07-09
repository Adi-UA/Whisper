import {
  Box, Button, Flex, Heading, HStack, Spinner, Text, VStack, useToast,
} from '@chakra-ui/react'
import { useEffect, useState } from 'react'
import { fetchGroups, triggerRotate, logout, type Group } from '../api'
import { GroupCard } from './GroupCard'
import { CreateGroupModal } from './CreateGroupModal'
import { JoinGroupModal } from './JoinGroupModal'

export function Dashboard() {
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(true)
  const [rotating, setRotating] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [joinOpen, setJoinOpen] = useState(false)
  const toast = useToast()

  async function loadGroups() {
    setLoading(true)
    try {
      setGroups(await fetchGroups())
    } catch {
      // fetchGroups redirects to OAuth on 401
    } finally {
      setLoading(false)
    }
  }

  async function handleRotate() {
    setRotating(true)
    try {
      const result = await triggerRotate()
      toast({
        title: `Rotated ${result.rotated} group${result.rotated !== 1 ? 's' : ''}`,
        status: 'success',
        duration: 3000,
      })
      await loadGroups()
    } catch {
      toast({ title: 'Rotation failed', status: 'error', duration: 3000 })
    } finally {
      setRotating(false)
    }
  }

  useEffect(() => { loadGroups() }, [])

  return (
    <Box minH="100vh" bg="gray.900" color="white">
      {/* Header */}
      <Box bg="gray.800" borderBottom="1px solid" borderColor="gray.700" px={8} py={4}>
        <Flex justify="space-between" align="center" maxW="1000px" mx="auto">
          <HStack spacing={3}>
            <Text fontSize="2xl">🤫</Text>
            <Heading size="lg" fontWeight="bold">Whisper</Heading>
          </HStack>
          <HStack spacing={3}>
            <Button size="sm" colorScheme="blue" onClick={() => setJoinOpen(true)}>
              Join group
            </Button>
            <Button size="sm" colorScheme="green" onClick={() => setCreateOpen(true)}>
              New group
            </Button>
            <Button
              size="sm"
              colorScheme="purple"
              onClick={handleRotate}
              isLoading={rotating}
              loadingText="…"
            >
              Rotate now
            </Button>
            <Button size="sm" variant="ghost" color="gray.400" onClick={logout}>
              Sign out
            </Button>
          </HStack>
        </Flex>
      </Box>

      {/* Content */}
      <Box maxW="1000px" mx="auto" p={8}>
        {loading ? (
          <Flex justify="center" mt={20}><Spinner size="xl" color="blue.300" /></Flex>
        ) : groups.length === 0 ? (
          <VStack mt={16} spacing={6} textAlign="center">
            <Text fontSize="6xl">🤫</Text>
            <Heading size="lg" color="gray.200">No groups yet</Heading>
            <Text color="gray.400" maxW="400px" lineHeight="tall">
              Create a group with your partner, family, or friends. Everyone gets
              the same secret phrase pushed to their phone daily.
            </Text>
            <HStack spacing={3} mt={2}>
              <Button colorScheme="green" size="lg" onClick={() => setCreateOpen(true)}>
                Create your first group
              </Button>
              <Button colorScheme="blue" variant="outline" size="lg" onClick={() => setJoinOpen(true)}>
                Join a group
              </Button>
            </HStack>
          </VStack>
        ) : (
          <VStack spacing={4} align="stretch">
            {groups.map(g => (
              <GroupCard key={g.id} group={g} onRefresh={loadGroups} />
            ))}
          </VStack>
        )}
      </Box>

      <CreateGroupModal
        isOpen={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={() => { setCreateOpen(false); loadGroups() }}
      />
      <JoinGroupModal
        isOpen={joinOpen}
        onClose={() => setJoinOpen(false)}
        onJoined={() => { setJoinOpen(false); loadGroups() }}
      />
    </Box>
  )
}

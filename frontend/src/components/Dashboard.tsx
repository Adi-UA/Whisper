import {
  Box, Button, Flex, Heading, Spinner, Text, VStack, useToast,
} from '@chakra-ui/react'
import { useEffect, useState } from 'react'
import { fetchGroups, triggerRotate, type Group } from '../api'
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
      // fetchGroups redirects to OAuth on 401, so errors here are real failures
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
    <Box minH="100vh" bg="gray.900" color="white" p={8}>
      <Flex justify="space-between" align="center" mb={8}>
        <Heading size="xl">🤫 Whisper</Heading>
        <Flex gap={3}>
          <Button colorScheme="blue" onClick={() => setJoinOpen(true)}>Join group</Button>
          <Button colorScheme="green" onClick={() => setCreateOpen(true)}>New group</Button>
          <Button
            colorScheme="purple"
            onClick={handleRotate}
            isLoading={rotating}
            loadingText="Rotating…"
          >
            Rotate now
          </Button>
        </Flex>
      </Flex>

      {loading ? (
        <Flex justify="center" mt={20}><Spinner size="xl" /></Flex>
      ) : groups.length === 0 ? (
        <VStack mt={20} spacing={3} opacity={0.6}>
          <Text fontSize="2xl">No groups yet</Text>
          <Text>Create a group or ask someone to share their join link.</Text>
        </VStack>
      ) : (
        <VStack spacing={4} align="stretch">
          {groups.map(g => (
            <GroupCard key={g.id} group={g} />
          ))}
        </VStack>
      )}

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

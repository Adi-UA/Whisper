import {
  Badge, Box, Button, Flex, Heading, HStack, Spinner,
  Table, Tbody, Td, Text, Th, Thead, Tr, VStack, useDisclosure, useToast,
} from '@chakra-ui/react'
import { useEffect, useState } from 'react'
import { deleteGroup, fetchHistory, fetchMembers, removeMember, type Group, type HistoryEntry, type Member } from '../api'

interface Props {
  group: Group
  onRefresh: () => void
}

export function GroupCard({ group, onRefresh }: Props) {
  const { isOpen, onToggle } = useDisclosure()
  const [members, setMembers] = useState<Member[]>([])
  const [history, setHistory] = useState<HistoryEntry[]>([])
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function load() {
    if (loading) return
    setLoading(true)
    try {
      const [m, h] = await Promise.all([
        fetchMembers(group.id),
        fetchHistory(group.id),
      ])
      setMembers(m)
      setHistory(h)
    } finally {
      setLoading(false)
    }
  }

  async function handleDeleteGroup() {
    if (!confirm(`Delete group "${group.name}"? This removes all members and history.`)) return
    try {
      await deleteGroup(group.id)
      toast({ title: 'Group deleted', status: 'info', duration: 2000 })
      onRefresh()
    } catch {
      toast({ title: 'Failed to delete group', status: 'error', duration: 3000 })
    }
  }

  async function handleRemoveMember(memberId: string, memberName: string) {
    if (!confirm(`Remove ${memberName} from this group?`)) return
    try {
      await removeMember(group.id, memberId)
      toast({ title: `${memberName} removed`, status: 'info', duration: 2000 })
      load()
    } catch {
      toast({ title: 'Failed to remove member', status: 'error', duration: 3000 })
    }
  }

  useEffect(() => { load() }, [group.id])

  const todayPhrase = history[0]?.phrase

  return (
    <Box bg="gray.800" borderRadius="xl" p={6} border="1px solid" borderColor="gray.700"
         _hover={{ borderColor: 'gray.600' }} transition="border-color 0.2s">

      {/* Header row */}
      <Flex justify="space-between" align="start">
        <VStack align="start" spacing={2}>
          <Heading size="md" color="gray.100">{group.name}</Heading>
          <HStack spacing={2}>
            <Badge colorScheme="blue" fontSize="xs">{group.schedule}</Badge>
            <Badge colorScheme="gray" fontSize="xs">{group.timezone}</Badge>
            <Badge colorScheme="green" fontSize="xs">{members.length} member{members.length !== 1 ? 's' : ''}</Badge>
          </HStack>
        </VStack>
        <Button size="xs" colorScheme="red" variant="ghost" onClick={handleDeleteGroup}>
          Delete
        </Button>
      </Flex>

      {/* Today's phrase — prominent */}
      {todayPhrase && (
        <Box mt={4} p={4} bg="gray.750" borderRadius="lg" border="1px solid" borderColor="green.800"
             bgGradient="linear(to-r, gray.800, gray.750)">
          <Text fontSize="xs" color="gray.500" textTransform="uppercase" letterSpacing="wide" mb={1}>
            Today's phrase
          </Text>
          <Text fontSize="2xl" fontWeight="bold" color="green.300" letterSpacing="wider" fontFamily="mono">
            {todayPhrase}
          </Text>
        </Box>
      )}

      {/* Expand/collapse details */}
      <Button size="sm" mt={4} variant="ghost" onClick={onToggle} color="gray.400" fontWeight="normal">
        {isOpen ? '▾ Hide details' : '▸ Show members & history'}
      </Button>

      {isOpen && (
        loading ? (
          <Spinner size="sm" mt={3} />
        ) : (
          <VStack align="stretch" mt={4} spacing={5}>
            {/* Members */}
            <Box>
              <Text fontWeight="semibold" mb={2} fontSize="sm" color="gray.400" textTransform="uppercase" letterSpacing="wide">
                Members
              </Text>
              <VStack align="stretch" spacing={2}>
                {members.map(m => (
                  <HStack key={m.id} bg="gray.700" p={3} borderRadius="md">
                    <Text flex={1} fontWeight="medium">{m.name}</Text>
                    <Text fontSize="xs" color="gray.500" fontFamily="mono">{m.channel}</Text>
                    <Button size="xs" colorScheme="red" variant="ghost"
                      onClick={() => handleRemoveMember(m.id, m.name)}>
                      ✕
                    </Button>
                  </HStack>
                ))}
                {members.length === 0 && (
                  <Text fontSize="sm" color="gray.500" fontStyle="italic">
                    No members yet. Share the group ID to invite people.
                  </Text>
                )}
              </VStack>
            </Box>

            {/* Join link */}
            <Box>
              <Text fontWeight="semibold" mb={1} fontSize="sm" color="gray.400" textTransform="uppercase" letterSpacing="wide">
                Group ID (share to invite)
              </Text>
              <Box bg="gray.700" p={2} borderRadius="md">
                <Text fontSize="sm" color="blue.300" fontFamily="mono" wordBreak="break-all">
                  {group.id}
                </Text>
              </Box>
            </Box>

            {/* History */}
            {history.length > 0 && (
              <Box>
                <Text fontWeight="semibold" mb={2} fontSize="sm" color="gray.400" textTransform="uppercase" letterSpacing="wide">
                  Phrase history
                </Text>
                <Table size="sm" variant="simple">
                  <Thead>
                    <Tr>
                      <Th color="gray.500" border="none">#</Th>
                      <Th color="gray.500" border="none">Phrase</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {history.map((h, i) => (
                      <Tr key={i}>
                        <Td color="gray.600" border="none" py={1}>{i + 1}</Td>
                        <Td fontFamily="mono" color="gray.300" border="none" py={1}>{h.phrase}</Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              </Box>
            )}
          </VStack>
        )
      )}
    </Box>
  )
}

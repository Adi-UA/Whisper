import {
  Badge, Box, Button, Heading, HStack, Spinner,
  Table, Tbody, Td, Text, Th, Thead, Tr, VStack, useDisclosure,
} from '@chakra-ui/react'
import { useEffect, useState } from 'react'
import { fetchHistory, fetchMembers, type Group, type HistoryEntry, type Member } from '../api'

interface Props {
  group: Group
}

export function GroupCard({ group }: Props) {
  const { isOpen, onToggle } = useDisclosure()
  const [members, setMembers] = useState<Member[]>([])
  const [history, setHistory] = useState<HistoryEntry[]>([])
  const [loading, setLoading] = useState(false)

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

  useEffect(() => { load() }, [group.id])

  const todayPhrase = history[0]?.phrase

  return (
    <Box bg="gray.800" borderRadius="lg" p={5} border="1px solid" borderColor="gray.700">
      <HStack justify="space-between" align="start">
        <VStack align="start" spacing={1}>
          <Heading size="md">{group.name}</Heading>
          <HStack>
            <Badge colorScheme="blue">{group.schedule}</Badge>
            <Badge colorScheme="gray">{group.timezone}</Badge>
          </HStack>
        </VStack>
        {todayPhrase && (
          <Box textAlign="right">
            <Text fontSize="xs" color="gray.400" mb={1}>today's phrase</Text>
            <Text fontSize="xl" fontWeight="bold" letterSpacing="wider" color="green.300">
              {todayPhrase}
            </Text>
          </Box>
        )}
      </HStack>

      <Button size="sm" mt={4} variant="ghost" onClick={onToggle} colorScheme="gray">
        {isOpen ? 'Hide details' : 'Show details'}
      </Button>

      {isOpen && (
        loading ? (
          <Spinner size="sm" mt={3} />
        ) : (
          <VStack align="stretch" mt={4} spacing={4}>
            {/* Members */}
            <Box>
              <Text fontWeight="semibold" mb={2} color="gray.300">Members ({members.length})</Text>
              <VStack align="stretch" spacing={1}>
                {members.map(m => (
                  <HStack key={m.id} bg="gray.700" p={2} borderRadius="md">
                    <Text flex={1}>{m.name}</Text>
                    <Text fontSize="xs" color="gray.400">{m.channel}</Text>
                  </HStack>
                ))}
              </VStack>
            </Box>

            {/* Join link */}
            <Box>
              <Text fontWeight="semibold" mb={1} color="gray.300">Join link</Text>
              <Text fontSize="sm" color="blue.300" fontFamily="mono" wordBreak="break-all">
                {window.location.origin}/join/{group.id}
              </Text>
            </Box>

            {/* History */}
            {history.length > 0 && (
              <Box>
                <Text fontWeight="semibold" mb={2} color="gray.300">Phrase history</Text>
                <Table size="sm" variant="simple">
                  <Thead>
                    <Tr>
                      <Th color="gray.400">#</Th>
                      <Th color="gray.400">Phrase</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {history.map((h, i) => (
                      <Tr key={i}>
                        <Td color="gray.500">{i + 1}</Td>
                        <Td fontFamily="mono">{h.phrase}</Td>
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

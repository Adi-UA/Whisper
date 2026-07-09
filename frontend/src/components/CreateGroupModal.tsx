import {
  Button, FormControl, FormHelperText, FormLabel, Input, Modal, ModalBody,
  ModalCloseButton, ModalContent, ModalFooter, ModalHeader, ModalOverlay,
  Select, useToast, VStack,
} from '@chakra-ui/react'
import { useState } from 'react'
import { createGroup } from '../api'

interface Props {
  isOpen: boolean
  onClose: () => void
  onCreated: () => void
}

const TIMEZONES = [
  'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
  'America/Toronto', 'Europe/London', 'Europe/Paris', 'Asia/Kolkata',
  'Asia/Tokyo', 'Australia/Sydney', 'UTC',
]

export function CreateGroupModal({ isOpen, onClose, onCreated }: Props) {
  const [name, setName] = useState('')
  const [schedule, setSchedule] = useState('daily')
  const [timezone, setTimezone] = useState(
    Intl.DateTimeFormat().resolvedOptions().timeZone || 'America/Chicago'
  )
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleCreate() {
    if (!name.trim()) return
    setLoading(true)
    try {
      await createGroup(name.trim(), schedule, timezone)
      toast({ title: 'Group created', status: 'success', duration: 2000 })
      setName('')
      onCreated()
    } catch {
      toast({ title: 'Failed to create group', status: 'error', duration: 3000 })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered>
      <ModalOverlay />
      <ModalContent bg="gray.800" color="white">
        <ModalHeader>New group</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <VStack spacing={4}>
            <FormControl isRequired>
              <FormLabel>Group name</FormLabel>
              <Input
                placeholder="e.g. Adi & GF"
                value={name}
                onChange={e => setName(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleCreate()}
                autoFocus
              />
            </FormControl>
            <FormControl>
              <FormLabel>Schedule</FormLabel>
              <Select value={schedule} onChange={e => setSchedule(e.target.value)}>
                <option value="daily">Daily (every day)</option>
                <option value="weekly">Weekly (every Monday)</option>
              </Select>
            </FormControl>
            <FormControl>
              <FormLabel>Timezone</FormLabel>
              <Select value={timezone} onChange={e => setTimezone(e.target.value)}>
                {TIMEZONES.map(tz => (
                  <option key={tz} value={tz}>{tz}</option>
                ))}
              </Select>
              <FormHelperText color="gray.400">
                For weekly groups, rotation fires on Monday in this timezone.
              </FormHelperText>
            </FormControl>
          </VStack>
        </ModalBody>
        <ModalFooter gap={2}>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button colorScheme="green" isLoading={loading} onClick={handleCreate}>
            Create
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  )
}

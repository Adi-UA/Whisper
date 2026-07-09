import {
  Button, FormControl, FormHelperText, FormLabel, Input, Modal, ModalBody,
  ModalCloseButton, ModalContent, ModalFooter, ModalHeader, ModalOverlay,
  useToast, VStack,
} from '@chakra-ui/react'
import { useState } from 'react'
import { joinGroup } from '../api'

interface Props {
  isOpen: boolean
  onClose: () => void
  onJoined: () => void
}

export function JoinGroupModal({ isOpen, onClose, onJoined }: Props) {
  const [groupId, setGroupId] = useState('')
  const [name, setName] = useState('')
  const [ntfyTopic, setNtfyTopic] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleJoin() {
    if (!groupId.trim() || !name.trim() || !ntfyTopic.trim()) return
    setLoading(true)
    try {
      await joinGroup(groupId.trim(), name.trim(), ntfyTopic.trim())
      toast({ title: 'Joined group', status: 'success', duration: 2000 })
      setGroupId('')
      setName('')
      setNtfyTopic('')
      onJoined()
    } catch {
      toast({ title: 'Failed to join group', status: 'error', duration: 3000 })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered>
      <ModalOverlay />
      <ModalContent bg="gray.800" color="white">
        <ModalHeader>Join a group</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <VStack spacing={4}>
            <FormControl isRequired>
              <FormLabel>Group ID</FormLabel>
              <Input
                placeholder="Paste the group ID from the invite link"
                value={groupId}
                onChange={e => setGroupId(e.target.value)}
                autoFocus
              />
            </FormControl>
            <FormControl isRequired>
              <FormLabel>Your name</FormLabel>
              <Input
                placeholder="How others see you"
                value={name}
                onChange={e => setName(e.target.value)}
              />
            </FormControl>
            <FormControl isRequired>
              <FormLabel>ntfy topic</FormLabel>
              <Input
                placeholder="e.g. whisper-adi-secret"
                value={ntfyTopic}
                onChange={e => setNtfyTopic(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleJoin()}
              />
              <FormHelperText color="gray.400">
                Subscribe to this topic in the ntfy app to receive push notifications.
                Pick something private and unguessable.
              </FormHelperText>
            </FormControl>
          </VStack>
        </ModalBody>
        <ModalFooter gap={2}>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button colorScheme="blue" isLoading={loading} onClick={handleJoin}>
            Join
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  )
}

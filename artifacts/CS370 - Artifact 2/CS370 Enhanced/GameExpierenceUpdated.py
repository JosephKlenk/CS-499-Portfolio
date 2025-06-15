import numpy as np
import heapq

class GameExperience(object):
    
    def __init__(self, model, max_memory=100, discount=0.95, use_priority=False):
        """
        GameExperience with both original and enhanced methods
        
        Args:
            model: neural network model
            max_memory: number of episodes to keep in memory  
            discount: discount factor for future rewards
            use_priority: False = original method, True = enhanced priority queue
        """
        self.model = model
        self.max_memory = max_memory
        self.discount = discount
        self.use_priority = use_priority
        self.num_actions = model.output_shape[-1]
        
        if use_priority:
            # Enhanced: Priority queue implementation
            self.priority_memory = []  # heap: (priority, index, episode)
            self.episode_count = 0
            print("Using ENHANCED Priority Queue Experience Replay")
        else:
            # Original: Simple list implementation  
            self.memory = list()
            print("Using ORIGINAL Random Sampling Experience Replay")
    
    def remember(self, episode):
        """Store episode - routes to original or enhanced method"""
        if self.use_priority:
            self._remember_enhanced(episode)
        else:
            self._remember_original(episode)
    
    def _remember_original(self, episode):
        """ORIGINAL: Simple list storage"""
        self.memory.append(episode)
        if len(self.memory) > self.max_memory:
            del self.memory[0]
    
    def _remember_enhanced(self, episode):
        """ENHANCED: Priority-based storage using TD-error"""
        # Calculate TD-error for priority
        td_error = self._calculate_td_error(episode)
        priority = abs(td_error) + 0.01  # Avoid zero priority
        
        # Store with priority (negative for max-heap)
        heapq.heappush(self.priority_memory, (-priority, self.episode_count, episode))
        self.episode_count += 1
        
        # Maintain memory limit
        if len(self.priority_memory) > self.max_memory:
            heapq.heappop(self.priority_memory)
    
    def _calculate_td_error(self, episode):
        """Calculate TD-error for priority calculation"""
        envstate, action, reward, envstate_next, game_over = episode
        
        current_q = self.predict(envstate)[action]
        
        if game_over:
            target_q = reward
        else:
            target_q = reward + self.discount * np.max(self.predict(envstate_next))
        
        return abs(target_q - current_q)
    
    def predict(self, envstate):
        """Predict Q-values for given environment state"""
        return self.model.predict(envstate)[0]

    def get_data(self, data_size=10):
        """Get training data - routes to original or enhanced method"""
        if self.use_priority:
            return self._get_data_enhanced(data_size)
        else:
            return self._get_data_original(data_size)
    
    def _get_data_original(self, data_size=10):
        """ORIGINAL: Random sampling from memory"""
        env_size = self.memory[0][0].shape[1]
        mem_size = len(self.memory)
        data_size = min(mem_size, data_size)
        inputs = np.zeros((data_size, env_size))
        targets = np.zeros((data_size, self.num_actions))
        
        for i, j in enumerate(np.random.choice(range(mem_size), data_size, replace=False)):
            envstate, action, reward, envstate_next, game_over = self.memory[j]
            inputs[i] = envstate
            targets[i] = self.predict(envstate)
            Q_sa = np.max(self.predict(envstate_next))
            if game_over:
                targets[i, action] = reward
            else:
                targets[i, action] = reward + self.discount * Q_sa
        return inputs, targets
    
    def _get_data_enhanced(self, data_size=10):
        """ENHANCED: Priority-based sampling"""
        if len(self.priority_memory) == 0:
            return None, None
            
        # Sample based on priority
        sample_size = min(data_size, len(self.priority_memory))
        memory_list = list(self.priority_memory)
        
        # Extract priorities and create probability distribution
        priorities = np.array([abs(item[0]) for item in memory_list])
        if priorities.sum() > 0:
            probabilities = priorities / priorities.sum()
        else:
            probabilities = np.ones(len(priorities)) / len(priorities)
        
        # Sample episodes
        sample_indices = np.random.choice(len(memory_list), sample_size, 
                                        p=probabilities, replace=False)
        sampled_episodes = [memory_list[i][2] for i in sample_indices]
        
        # Prepare training data
        env_size = sampled_episodes[0][0].shape[1]
        inputs = np.zeros((sample_size, env_size))
        targets = np.zeros((sample_size, self.num_actions))
        
        for i, episode in enumerate(sampled_episodes):
            envstate, action, reward, envstate_next, game_over = episode
            inputs[i] = envstate
            targets[i] = self.predict(envstate)
            
            if game_over:
                targets[i, action] = reward
            else:
                Q_sa = np.max(self.predict(envstate_next))
                targets[i, action] = reward + self.discount * Q_sa
                
        return inputs, targets
    
    def get_stats(self):
        """Get statistics about the experience replay"""
        if self.use_priority:
            return {
                'method': 'Priority Queue',
                'memory_size': len(self.priority_memory),
                'total_experiences': self.episode_count
            }
        else:
            return {
                'method': 'Random Sampling', 
                'memory_size': len(self.memory)
            }
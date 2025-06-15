#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
CRUD module for interacting with MongoDB in the Grazioso Salvare dashboard application
This module handles all database operations for the animal shelter data
Created by: Joseph Klenk
"""

from pymongo import MongoClient
from bson.objectid import ObjectId

class AnimalShelter(object):
    """ CRUD operations for Animal collection in MongoDB """
    
    def __init__(self, username, password, host='nv-desktop-services.apporto.com', port=33270):
        """
        Initialize connection to database
        
        Args:
            username (str): MongoDB username
            password (str): MongoDB password
            host (str): MongoDB host address
            port (int): MongoDB port number
        """
        # Initializing the MongoClient. This helps to 
        # access the MongoDB databases and collections.
        # Connection string with authentication
        try:
            print(f"Connecting to MongoDB at {host}:{port}")
            self.client = MongoClient(f'mongodb://{username}:{password}@{host}:{port}/?authSource=AAC')
            self.database = self.client['AAC']
            print("Connected to MongoDB successfully")
        except Exception as e:
            print(f"Error connecting to MongoDB: {e}")
            raise
        
    def create(self, data):
        """
        Create/insert a document into the animals collection
        
        Parameters:
            data (dict): The document to be inserted
        
        Returns:
            bool: True if the document was inserted successfully, False otherwise
        """
        if data is not None:
            try:
                self.database.animals.insert_one(data)  # data should be dictionary
                return True
            except Exception as e:
                print(f"An error occurred during creation: {e}")
                return False
        else:
            raise Exception("Nothing to save, because data parameter is empty")
            return False
    
    def read(self, criteria=None, projection=None):
        """
        Read/query document(s) from the animals collection
        
        Parameters:
            criteria (dict): The query criteria to find documents.
                           If None, returns all documents.
            projection (dict): The fields to include or exclude in the result
        
        Returns:
            list: The results of the query as a list, empty list if no documents found
        """
        if criteria is None:
            criteria = {}
            
        try:
            cursor = self.database.animals.find(criteria, projection)
            return list(cursor)
        except Exception as e:
            print(f"An error occurred during read operation: {e}")
            return []
            
    def update(self, criteria, update_data):
        """
        Update document(s) in the animals collection
        
        Parameters:
            criteria (dict): The query criteria to find documents for update
            update_data (dict): The update operations to apply
        
        Returns:
            int: The number of modified documents
        """
        if criteria is None:
            raise Exception("No criteria provided for update")
            return 0
            
        try:
            result = self.database.animals.update_many(criteria, {"$set": update_data})
            return result.modified_count
        except Exception as e:
            print(f"An error occurred during update: {e}")
            return 0
            
    def delete(self, criteria):
        """
        Delete document(s) from the animals collection
        
        Parameters:
            criteria (dict): The query criteria to find documents for deletion
        
        Returns:
            int: The number of deleted documents
        """
        if criteria is None:
            raise Exception("No criteria provided for deletion")
            return 0
            
        try:
            result = self.database.animals.delete_many(criteria)
            return result.deleted_count
        except Exception as e:
            print(f"An error occurred during deletion: {e}")
            return 0
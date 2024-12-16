package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Users;

//Repositoryインターフェース（具体的にはJpaRepository）を利用すると、データベースへのCRUD（Create, Read, Update, Delete）操作が簡単に実装できます
public interface UserRepository extends JpaRepository<Users, Long>{

}

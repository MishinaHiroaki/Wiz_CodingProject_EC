package com.example.demo.controller;

import java.security.MessageDigest;//追加
import java.security.NoSuchAlgorithmException;//追加
import java.util.Base64;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Users;
import com.example.demo.service.UserService;

@Controller
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/") //ログイン画面を表示
	public String showRegisterForm() {
		return "register"; //register.htmlを表示
	}

	@GetMapping("/users") //登録したユーザを一覧できる
	public String getUsers(Model model) {
		List<Users> users = userService.getAllUsers();
		model.addAttribute("users", users);
		return "users";//users.html(登録完了画面)を表示
	}

	@PostMapping("/register") // ログイン画面から呼び出される
	public String registerUser(@RequestParam String action, @ModelAttribute Users user, Model model,
			HttpSession session) throws NoSuchAlgorithmException {
		if (user.getName().trim().equals("") || user.getPassword().trim().equals("")) { //名前とパスワードが空の時は登録できない
			model.addAttribute("message", "名前またはパスワードが空です");
			return "loginFailure";//loginFailure.html(ログイン失敗画面)を表示
		}

		if ("register0".equals(action)) { //新規登録ボタンが押下された時に呼ばれる
			List<Users> users = userService.getAllUsers();
			for (Users existingUser : users) {

				if (existingUser.getName().equals(user.getName())) { //既に登録済の名前で登録しようとすると、呼ばれる
					model.addAttribute("message", "その名前は既に使用されています");
					return "register"; //ログイン画面を表示
				}
			}
			//追加した箇所ここから
			// 1. ハッシュ化したいデータ
			String data = user.getPassword();
			// 2. インスタンスの取得
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			//3.入力データ（バイト配列）を渡してハッシュ化を実行
			byte[] hashBytes = md.digest(data.getBytes());

			// 4.バイト配列をBase64文字列に変換
			String base64Hash = Base64.getEncoder().encodeToString(hashBytes);
			// 5.変換後のハッシュ文字列をセット
			user.setPassword(base64Hash);
			//ここまで	

			Users newuser = userService.saveUser(user);//userServiceクラスのsaveUserメソッドを呼び出して、データベースに登録する

			session.setAttribute("userid", newuser.getId()); //セッションオブジェクトに今データベースに登録したidを格納する

			model.addAttribute("user", newuser); //modelに今データベースに登録したid,name,passwordを格納する

			return "user"; //users.html(登録完了画面)を表示
		} else if ("login".equals(action)) { //ログインボタンが押下された時に呼ばれる
			List<Users> users = userService.getAllUsers(); //userServiceクラスのgetAllUsersメソッドを呼び出し。登録した全てのユーザを持ってくる。
			for (Users existingUser : users) {

				if (existingUser.getName().equals(user.getName())) { //登録したnaneとログイン画面から送信された名前が等しい時、if文の中、実行される
					//追加した箇所ここから
					// 1. ハッシュ化したいデータ
					String data2 = user.getPassword();
					// 2. インスタンスの取得
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					//3.入力データ（バイト配列）を渡してハッシュ化を実行
					byte[] hashBytes = md.digest(data2.getBytes());
					// 4.バイト配列をBase64文字列に変換
					String base64Hash2 = Base64.getEncoder().encodeToString(hashBytes);
					//ここまで	

					if (base64Hash2.equals(existingUser.getPassword())) { //そのnameのpasswordとログイン画面から送信されたパスワードが等しい時、if文の中、実行される
						session.setAttribute("userid", existingUser.getId()); //セッションオブジェクトにデータベースにnameとpasswordが一致しているidを格納する
						return "login"; //TOP画面を表示
					} else {
						model.addAttribute("message", "パスワードが間違っています");//登録したnaneとログイン画面から送信された名前が等しいが、パスワードが誤りの時に実行される
						return "loginFailure";//ログイン失敗画面を表示
					}
				}
			}

		}
		model.addAttribute("message", "その名前で登録情報がありません");//登録したnaneとログイン画面から送信された名前が誤り、パスワードが誤りの時に実行される
		return "loginFailure";//ログイン失敗画面を表示

	}

	@GetMapping("/edit") //TOP画面のパスワード編集ボタンを押下すると呼ばれる
	public String editPass() {
		return "edit";//パスワード変更画面を表示
	}

	@PostMapping("/edit-password") //パスワード変更画面の変更ボタンを押下すると呼ばれる。現在のパスワード(oldpassword)と新しいパスワード(password)を受け取る
	public String editPassPost(@RequestParam String oldpassword, @RequestParam String password, HttpSession session,
			Model model) throws NoSuchAlgorithmException {

		Long userid = (Long) session.getAttribute("userid");//セッションオブジェクトに格納されたidを取り出し、useridに格納する
		Users user = userService.findUserById(userid);//userServiceクラスのfindUserByIdメソッドを呼び出す、引数はセッションオブジェクトに格納されたid。そのidの持つnameとpasswordの情報をuserに格納。
		//追加した箇所ここから
		// 1. ハッシュ化したいデータ
		String data3 = oldpassword;
		// 2. インスタンスの取得
		MessageDigest md3 = MessageDigest.getInstance("SHA-256");
		//3.入力データ（バイト配列）を渡してハッシュ化を実行
		byte[] hashBytes3 = md3.digest(data3.getBytes());
		// 4.バイト配列をBase64文字列に変換
		String base64Hash_oldpassword = Base64.getEncoder().encodeToString(hashBytes3);
		//ここまで	
		if (user.getPassword().equals(base64Hash_oldpassword)) { //userのpasswordと現在のパスワードが等しい時、以下実行される
			//追加した箇所ここから
			// 1. ハッシュ化したいデータ
			String data4 = password;
			// 2. インスタンスの取得
			MessageDigest md4 = MessageDigest.getInstance("SHA-256");
			//3.入力データ（バイト配列）を渡してハッシュ化を実行
			byte[] hashBytes4 = md4.digest(data4.getBytes());
			// 4.バイト配列をBase64文字列に変換
			String base64Hash_newpassword = Base64.getEncoder().encodeToString(hashBytes4);
			//ここまで
			user.setPassword(base64Hash_newpassword); //新しいパスワードをuserに格納する

			userService.saveUser(user);//userServiceクラスのsaveUserメソッドを実行し、パスワードを書き換えた情報に更新する

			model.addAttribute("user", user);//modelに更新したエンティティを送る
			return "user";//user.html(登録完了画面)を表示
		} else {
			model.addAttribute("message", "現在のパスワードが間違っています。");//userのpasswordと現在のパスワードが誤り時、以下実行される
			return "loginFailure";//ログイン失敗画面を表示
		}

	}

}
